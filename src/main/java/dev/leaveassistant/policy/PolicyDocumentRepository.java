package dev.leaveassistant.policy;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    Optional<PolicyDocument> findByName(String name);
}
