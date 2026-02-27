package com.example.unis_rssol.domain.todo;

import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.store.StoreRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.todo.dto.TodoCreateRequestDto;
import com.example.unis_rssol.domain.todo.dto.TodoListResponseDto;
import com.example.unis_rssol.domain.todo.dto.TodoResponseDto;
import com.example.unis_rssol.domain.todo.dto.TodoUpdateRequestDto;
import com.example.unis_rssol.domain.user.User;
import com.example.unis_rssol.domain.user.UserRepository;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AuthorizationService authorizationService;

    /**
     * 특정 날짜의 모든 할일 조회 (타입별로 분류)
     */
    public TodoListResponseDto getTodosByDate(Long userId, LocalDate date) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);


        List<Todo> allTodos = todoRepository.findAllTodosForDate(storeId, userId, date);

        List<TodoResponseDto> storeTodos = allTodos.stream()
                .filter(t -> t.getTodoType() == Todo.TodoType.STORE)
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());

        List<TodoResponseDto> handoverTodos = allTodos.stream()
                .filter(t -> t.getTodoType() == Todo.TodoType.HANDOVER)
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());

        List<TodoResponseDto> personalTodos = allTodos.stream()
                .filter(t -> t.getTodoType() == Todo.TodoType.PERSONAL)
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());

        return TodoListResponseDto.builder()
                .date(date)
                .storeTodos(storeTodos)
                .handoverTodos(handoverTodos)
                .personalTodos(personalTodos)
                .build();
    }

    /**
     * 할일 생성
     */
    @Transactional
    public TodoResponseDto createTodo(Long userId, TodoCreateRequestDto request) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authorizationService.getUserStoreOrThrow(userId, storeId);

        // 권한 체크
        validateCreatePermission(userStore, request.getTodoType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("매장을 찾을 수 없습니다."));

        Todo todo = Todo.builder()
                .store(store)
                .user(user)
                .date(request.getDate())
                .todoType(request.getTodoType())
                .content(request.getContent())
                .completed(false)
                .build();

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponseDto.from(savedTodo);
    }

    /**
     * 할일 수정
     */
    @Transactional
    public TodoResponseDto updateTodo(Long userId, Long todoId, TodoUpdateRequestDto request) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authorizationService.getUserStoreOrThrow(userId, storeId);

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));

        // 매장 확인
        if (!todo.getStore().getId().equals(storeId)) {
            throw new ForbiddenException("해당 매장의 할일이 아닙니다.");
        }

        // 권한 체크
        validateUpdateDeletePermission(userStore, todo, userId);

        if (request.getContent() != null) {
            todo.setContent(request.getContent());
        }
        if (request.getCompleted() != null) {
            todo.setCompleted(request.getCompleted());
        }

        return TodoResponseDto.from(todo);
    }

    /**
     * 할일 삭제
     */
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authorizationService.getUserStoreOrThrow(userId, storeId);

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));

        // 매장 확인
        if (!todo.getStore().getId().equals(storeId)) {
            throw new ForbiddenException("해당 매장의 할일이 아닙니다.");
        }

        // 권한 체크
        validateUpdateDeletePermission(userStore, todo, userId);

        todoRepository.delete(todo);
    }

    /**
     * 할일 완료 토글
     */
    @Transactional
    public TodoResponseDto toggleTodoCompleted(Long userId, Long todoId) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authorizationService.getUserStoreOrThrow(userId, storeId);

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));

        // 매장 확인
        if (!todo.getStore().getId().equals(storeId)) {
            throw new ForbiddenException("해당 매장의 할일이 아닙니다.");
        }

        // 권한 체크
        validateUpdateDeletePermission(userStore, todo, userId);

        todo.setCompleted(!todo.getCompleted());

        return TodoResponseDto.from(todo);
    }

    /**
     * 생성 권한 검증
     * - STORE: OWNER만
     * - HANDOVER: OWNER, STAFF 모두
     * - PERSONAL: 모든 사용자 (본인 것만)
     */
    private void validateCreatePermission(UserStore userStore, Todo.TodoType todoType) {
        if (todoType == Todo.TodoType.STORE) {
            if (userStore.getPosition() != UserStore.Position.OWNER) {
                throw new ForbiddenException("매장 전체 할일은 OWNER만 추가할 수 있습니다.");
            }
        }
        // HANDOVER, PERSONAL은 모두 가능
    }

    /**
     * 수정/삭제 권한 검증
     * - STORE: OWNER만
     * - HANDOVER: 작성자 또는 OWNER
     * - PERSONAL: 작성자 본인만
     */
    private void validateUpdateDeletePermission(UserStore userStore, Todo todo, Long userId) {
        boolean isOwner = userStore.getPosition() == UserStore.Position.OWNER;
        boolean isAuthor = todo.getUser().getId().equals(userId);

        switch (todo.getTodoType()) {
            case STORE:
                if (!isOwner) {
                    throw new ForbiddenException("매장 전체 할일은 OWNER만 수정/삭제할 수 있습니다.");
                }
                break;
            case HANDOVER:
                if (!isOwner && !isAuthor) {
                    throw new ForbiddenException("인수인계는 작성자 또는 OWNER만 수정/삭제할 수 있습니다.");
                }
                break;
            case PERSONAL:
                if (!isAuthor) {
                    throw new ForbiddenException("내 할일은 본인만 수정/삭제할 수 있습니다.");
                }
                break;
        }
    }
}

