package com.yourapp.repository;

import com.yourapp.model.ProgressPhoto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProgressPhotoRepository extends MongoRepository<ProgressPhoto, String> {

    List<ProgressPhoto> findByUserIdOrderByPhotoDateDescCreatedAtDesc(String userId);
}
