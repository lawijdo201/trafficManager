package gwnucapstone.trafficmanager.service.Impl;

import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.dto.MailDTO;
import gwnucapstone.trafficmanager.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserDAO userDAO;
    private final BCryptPasswordEncoder encoder;
    private final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, UserDAO userDAO, BCryptPasswordEncoder encoder) {
        this.mailSender = mailSender;
        this.userDAO = userDAO;
        this.encoder = encoder;
    }

    @Override
    public void sendEmail(MailDTO mailDto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailDto.getEmail());
        message.setSubject(mailDto.getTitle());
        message.setText(mailDto.getMessage());

        mailSender.send(message);
        LOGGER.info("[sendEmailForId] 이메일 전송 완료");
    }

    @Override
    public MailDTO createMessageForId(String email, String id, String name) {
        MailDTO dto = new MailDTO();
        dto.setEmail(email);
        dto.setTitle("아이디 찾기 이메일입니다.");
        dto.setMessage("안녕하세요. " + name + "님. \n회원님의 아이디는 " + id + "입니다.");
        return dto;
    }

    @Override
    public MailDTO createMessageForPw(String email, String id, String name) {
        String pw = getTempPassword();
        MailDTO dto = new MailDTO();
        dto.setEmail(email);
        dto.setTitle("비밀번호 찾기 이메일입니다.");
        dto.setMessage("안녕하세요. " + name + "(" + id + ")님. \n회원님의 임시 비밀번호는 " + pw + "입니다.\n" +
                "해당 비밀번호는 임시 비밀번호이므로 로그인 후 변경해주세요.");
        updatePassword(pw, id);
        return dto;
    }

    public void updatePassword(String pw, String id) {
        String modifiedPw = encoder.encode(pw);
        userDAO.updateUserPassword(id, modifiedPw);
    }

    public String getTempPassword() {
        char[] charSet = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
                'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        char[] symbolSet = new char[]{'$', '@', '$', '!', '%', '*', '#', '?', '&'};

        StringBuilder password = new StringBuilder();

        int idx = 0;
        int symbol = (int) (9 * Math.random());
        for (int i = 0; i < 10; i++) {
            if (i == symbol) {
                idx = (int) (symbolSet.length * Math.random());
                password.append(symbolSet[idx]);
            } else {
                idx = (int) (charSet.length * Math.random());
                password.append(charSet[idx]);
            }
        }
        return password.toString();
    }
}
