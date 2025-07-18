package goodspace.backend.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        // GreenMail 기본 SMTP 포트(3025)로 오버라이드
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        // 로컬 테스트이므로 인증 없이 보낼 수 있도록
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false"
})
class LocalEmailTest {
    private static final String MAIL_TITLE = "로컬 이메일 전송 테스트";
    private static final String MAIL_BODY = "Spring Boot + GreenMail 테스트입니다";
    private static final String MAIL_RECIPIENT = "hello@mail.com";

    // GreenMailExtension 으로 in‑memory SMTP 서버 실행
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    JavaMailSender mailSender;

    @Test
    @DisplayName("로컬 환경에서 메일을 전송한다")
    void javaMailSender_shouldSendMailToGreenMail() throws MessagingException, IOException {
        MimeMessage message = createMessage(MAIL_TITLE, MAIL_BODY, MAIL_RECIPIENT);

        mailSender.send(message);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);

        MimeMessage mail = received[0];
        assertThat(isSameMessage(mail, MAIL_TITLE, MAIL_BODY, MAIL_RECIPIENT)).isTrue();
    }

    private MimeMessage createMessage(String title, String body, String targetEmail) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        message.setRecipient(RecipientType.TO, new InternetAddress(targetEmail));
        message.setSubject(title);
        message.setText(body);

        return message;
    }

    private boolean isSameMessage(MimeMessage mail, String title, String body, String recipient) throws MessagingException, IOException {
        String actualTitle = mail.getSubject();
        String actualBody = (String) mail.getContent();
        String actualRecipient = mail.getAllRecipients()[0].toString();

        return actualTitle.equals(title) &&
                actualBody.equals(body)&&
                actualRecipient.equals(recipient);
    }
}
