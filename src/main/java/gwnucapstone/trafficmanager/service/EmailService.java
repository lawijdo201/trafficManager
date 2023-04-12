package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.MailDTO;

public interface EmailService {

    void sendEmail(MailDTO mailDto);

    MailDTO createMessageForId(String email, String id, String name);

    MailDTO createMessageForPw(String email, String id, String name);
}
