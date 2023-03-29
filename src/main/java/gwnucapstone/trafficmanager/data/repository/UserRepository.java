package gwnucapstone.trafficmanager.data.repository;

import gwnucapstone.trafficmanager.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByid(String id);
}
