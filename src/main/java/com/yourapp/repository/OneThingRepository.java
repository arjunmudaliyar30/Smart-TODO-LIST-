package com.yourapp.repository;

import com.yourapp.model.OneThing;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface OneThingRepository extends MongoRepository<OneThing, String> {

    Optional<OneThing> findByUserIdAndDate(String userId, LocalDate date);
}
