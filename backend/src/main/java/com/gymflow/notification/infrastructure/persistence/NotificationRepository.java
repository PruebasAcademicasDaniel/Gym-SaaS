package com.gymflow.notification.infrastructure.persistence;

import com.gymflow.notification.domain.Notification;
import com.gymflow.notification.domain.NotificationType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByMembershipIdAndType(UUID membershipId, NotificationType type);
}
