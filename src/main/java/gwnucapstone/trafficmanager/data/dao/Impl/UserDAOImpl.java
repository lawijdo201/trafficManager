package gwnucapstone.trafficmanager.data.dao.Impl;

import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.data.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserDAOImpl implements UserDAO {
    private final UserRepository userRepository;

    @Autowired
    public UserDAOImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void saveMember(User user) {
        userRepository.save(user);
    }

    @Override
    public boolean findMember(String name) {
        return userRepository.existsByid(name);
    }

    @Override
    public Optional<User> findByid(String id) {
        return userRepository.findById(id);
    }

    @Override
    public void deleteMember(String id) {
        userRepository.deleteById(id);
    }

    /**
     * 사용자 정보 업데이트 메서드
     *
     * @param id    사용자 아이디
     * @param pw    현재 비밀번호
     * @param email 사용자 이메일
     */
    @Override
    @Transactional
    public void updateMember(String id, String pw, String email) {
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPw(pw);
            user.setEmail(email);
            userRepository.save(user);
        }
    }

    /**
     * 아이디, 비밀번호 찾기 시 이름과 이메일로 사용자 찾기
     *
     * @param name  사용자 이름
     * @param email 사용자 이메일
     * @return
     */
    @Override
    public Optional<User> findByNameAndEmail(String name, String email) {
        return userRepository.findByNameAndEmail(name, email);
    }

    /**
     * 임시 비밀번호로 유저 정보 업데이트
     *
     * @param id 사용자 아이디
     * @param pw 임시 비밀번호
     */
    @Override
    public void updateUserPassword(String id, String pw) {
        userRepository.updatePw(id, pw);
    }
}
