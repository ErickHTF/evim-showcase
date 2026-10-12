package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.AtlassianUserToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtlassianUserTokenRepository extends JpaRepository<AtlassianUserToken, String> {
}
