package com.example.EVIM.adapter.out.graph;

import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.port.out.EmailSendPort;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "graph.mock.enabled", havingValue = "false", matchIfMissing = true)
public class GraphMailSendAdapter implements EmailSendPort {

    private final GraphServiceClient graphClient;

    @Value("${graph.mailbox-user-id}")
    private String mailboxUserId;

    public GraphMailSendAdapter(GraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    @Override
    public void sendOnBehalf(String displayName, String to, String subject, String htmlBody, List<Attachment> attachments) {
        Message message = new Message();
        message.setSubject(subject);

        ItemBody body = new ItemBody();
        body.setContentType(BodyType.Html);
        body.setContent(htmlBody);
        message.setBody(body);

        Recipient from = new Recipient();
        EmailAddress fromAddress = new EmailAddress();
        fromAddress.setName(displayName);
        fromAddress.setAddress(mailboxUserId);
        from.setEmailAddress(fromAddress);
        message.setFrom(from);

        Recipient recipient = new Recipient();
        EmailAddress recipientAddress = new EmailAddress();
        recipientAddress.setAddress(to);
        recipient.setEmailAddress(recipientAddress);
        message.setToRecipients(List.of(recipient));

        if (attachments != null && !attachments.isEmpty()) {
            List<com.microsoft.graph.models.Attachment> graphAttachments = new ArrayList<>();
            for (Attachment att : attachments) {
                FileAttachment fa = new FileAttachment();
                fa.setName(att.getFileName());
                fa.setContentType(att.getContentType());
                fa.setContentBytes(att.getContentBytes());
                fa.setOdataType("#microsoft.graph.fileAttachment");
                graphAttachments.add(fa);
            }
            message.setAttachments(graphAttachments);
        }

        SendMailPostRequestBody sendMailBody = new SendMailPostRequestBody();
        sendMailBody.setMessage(message);
        sendMailBody.setSaveToSentItems(true);

        graphClient.users().byUserId(mailboxUserId)
                .sendMail()
                .post(sendMailBody);
    }
}
