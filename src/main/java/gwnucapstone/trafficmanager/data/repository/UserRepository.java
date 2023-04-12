package gwnucapstone.trafficmanager.data.repository;

import gwnucapstone.trafficmanager.data.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByid(String id);

    Optional<User> findByNameAndEmail(String name, String email);

    @Transactional
    @Modifying
    @Query(value = "update User u set u.pw=:pw where u.id=:id")
    void updatePw(@Param("id") String id, @Param("pw") String pw);
}
