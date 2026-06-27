package com.example.aution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.ItemDetailsEntity;

@Repository
public interface ItemDetailsRepository extends JpaRepository<ItemDetailsEntity, Long> {
}