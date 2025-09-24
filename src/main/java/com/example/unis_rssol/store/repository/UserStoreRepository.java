package com.example.unis_rssol.store.repository;

import com.example.unis_rssol.store.entity.UserStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserStoreRepository extends JpaRepository<UserStore, Long> {}