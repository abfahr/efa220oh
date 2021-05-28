/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.Messages;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class EmailSenderThread extends Thread {

  private long lastScnAdmins = -1;
  private Vector<String> emailAddressesAdmin;
  private Vector<String> emailAddressesBoatMaintenance;
  private Vector<MultiPartMessage> multipartMessages = new Vector<MultiPartMessage>();

  private long lastScnEfaConfig = -1;
  private String serverUrl;
  private String serverPort;
  private String serverUsername;
  private String serverPassword;
  private String mailFromEmail;
  private String mailFromName;
  private String mailSubjectPrefix;
  private String mailSignature;

  // Constructor just for Plugin Check
  public EmailSenderThread() {
    javax.mail.Session session = javax.mail.Session.getInstance(new Properties(), null); // just
    // dummy
    // statement
  }

  public void enqueueMessage(javax.mail.Multipart message,
      Vector<String> addresses, String subject,
      String[] attachmentFileNames, boolean deleteAttachmentFiles) {
    multipartMessages.add(new MultiPartMessage(message, addresses, subject,
        attachmentFileNames, deleteAttachmentFiles));
    interrupt();
  }

  public boolean sendMessage(javax.mail.Multipart message,
      Vector<String> addresses, String subject,
      String[] attachmentFileNames, boolean deleteAttachmentFiles) {
    return sendMail(new MultiPartMessage(message, addresses, subject,
        attachmentFileNames, deleteAttachmentFiles));
  }

  private void updateMailProperties() {
    try {
      long scn = Daten.efaConfig.data().getSCN();
      if (scn == lastScnEfaConfig) {
        return;
      }
      serverUrl = Daten.efaConfig.getValueEfaDirekt_emailServer();
      serverPort = Integer.toString(Daten.efaConfig.getValueEfaDirekt_emailPort());
      serverUsername = Daten.efaConfig.getValueEfaDirekt_emailUsername();
      serverPassword = Daten.efaConfig.getValueEfaDirekt_emailPassword();
      mailFromEmail = Daten.efaConfig.getValueEfaDirekt_emailAbsender();
      mailFromName = Daten.efaConfig.getValueEfaDirekt_emailAbsenderName();
      mailSubjectPrefix = Daten.efaConfig.getValueEfaDirekt_emailBetreffPraefix();
      mailSignature = Daten.efaConfig.getValueEfaDirekt_emailSignatur();

      if (serverUrl != null && serverUrl.trim().length() == 0) {
        serverUrl = null;
      }
      if (serverUrl != null && serverUrl.contains("xxx")) {
        serverUrl = null;
      }
      if (serverPort != null && serverPort.trim().length() == 0) {
        serverPort = null;
      }
      if (serverUsername != null && serverUsername.trim().length() == 0) {
        serverUsername = null;
      }
      if (serverPassword != null && serverPassword.trim().length() == 0) {
        serverPassword = null;
      }
      if (mailFromEmail != null && mailFromEmail.trim().length() == 0) {
        mailFromEmail = null;
      }
      if (mailFromName != null && mailFromName.trim().length() == 0) {
        mailFromName = Daten.EFA_SHORTNAME;
      }
      if (mailSubjectPrefix != null && mailSubjectPrefix.trim().length() == 0) {
        mailSubjectPrefix = null;
      }
      if (mailSignature != null && mailSignature.trim().length() == 0) {
        mailSignature = null;
      }
      lastScnEfaConfig = scn;
    } catch (Exception e) {
      Logger.logdebug(e);
    }
  }

  private void updateAdminEmailAddresses() {
    try {
      long scn = Daten.admins.data().getSCN();
      if (scn == lastScnAdmins) {
        return;
      }
      emailAddressesAdmin = new Vector<String>();
      emailAddressesBoatMaintenance = new Vector<String>();
      DataKeyIterator it = Daten.admins.data().getStaticIterator();
      DataKey k = it.getFirst();
      while (k != null) {
        AdminRecord admin = (AdminRecord) Daten.admins.data().get(k);
        if (admin != null && admin.getEmail() != null && admin.getEmail().length() > 0) {
          if (admin.isAllowedMsgReadAdmin() && !emailAddressesAdmin.contains(admin.getEmail())) {
            emailAddressesAdmin.add(admin.getEmail());
          }
          if (admin.isAllowedMsgReadBoatMaintenance()
              && !emailAddressesBoatMaintenance.contains(admin.getEmail())) {
            emailAddressesBoatMaintenance.add(admin.getEmail());
          }
        }
        k = it.getNext();
      }
      if (emailAddressesAdmin.size() == 0) {
        emailAddressesAdmin = null;
      }
      if (emailAddressesBoatMaintenance.size() == 0) {
        emailAddressesBoatMaintenance = null;
      }
      lastScnAdmins = scn;
    } catch (Exception e) {
      Logger.logdebug(e);
    }
  }

  private boolean sendMail(MessageRecord msg, Vector<String> addresses) {
    try {
      StringBuilder recipients = new StringBuilder();
      for (int i = 0; i < addresses.size(); i++) {
        recipients.append((recipients.length() > 0 ? ", " : "") + addresses.get(i));
      }
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 3)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL,
            "Trying to send message " + msg.getMessageId() + " to " + recipients.toString()
                + " ...");
      }
      boolean auth = (serverUsername != null && serverPassword != null);
      String protocol = Daten.efaConfig.getValueEmailSSL() ? "smtps" : "smtp";
      Properties props = new Properties();
      props.put("mail." + protocol + ".host", serverUrl);
      props.put("mail." + protocol + ".port", serverPort);
      if (auth) {
        props.put("mail." + protocol + ".auth", "true");
      }
      // props.put("mail.smtp.ssl.trust", "*");
      // props.put("mail." + protocol + ".ssl.trust", "*");
      if (Daten.efaConfig.getValueEmailSSL()) {
        props.put("mail." + protocol + ".ssl.enable", "true");
      } else {
        props.put("mail." + protocol + ".starttls.enable", "true");
      }
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
        props.put("mail.debug", "true");
      }
      MailAuthenticator ma = null;
      if (auth) {
        ma = new MailAuthenticator(serverUsername, serverPassword);
      }
      String charset = Daten.ENCODING_ISO;
      javax.mail.Session session = javax.mail.Session.getInstance(props, ma);
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
        session.setDebugOut(Logger.getPrintStream());
      }
      com.sun.mail.smtp.SMTPMessage mail = new com.sun.mail.smtp.SMTPMessage(session);
      mail.setAllow8bitMIME(true);
      mail.setHeader("X-Mailer", Daten.EFA_SHORTNAME + " " + Daten.VERSIONID);
      mail.setHeader("Content-Type", "text/plain; charset=" + charset);
      mail.setFrom(new javax.mail.internet.InternetAddress(mailFromName + " <" + mailFromEmail
          + ">"));
      mail.setRecipients(RecipientType.TO,
          javax.mail.internet.InternetAddress.parse(recipients.toString()));
      if (msg.getReplyTo() != null && msg.getReplyTo().length() > 0) {
        try {
          mail.setReplyTo(new Address[] { new InternetAddress(msg.getReplyTo()) });
        } catch (Exception e) {
          Logger.logdebug(e);
        }
      }
      mail.setSubject(
          (mailSubjectPrefix != null ? "[" + mailSubjectPrefix + "] " : "") + msg.getSubject(),
          charset);
      mail.setSentDate(new Date());
      mail.setText("## " + International.getString("Absender") + ": " + msg.getFrom() + "\n"
          + "## " + International.getString("Betreff") + " : " + msg.getSubject() + "\n\n"
          + msg.getText()
          + (mailSignature != null ? "\n\n-- \n"
              + EfaUtil.replace(mailSignature, "$$", "\n", true) : ""),
          charset);
      com.sun.mail.smtp.SMTPTransport t = (com.sun.mail.smtp.SMTPTransport) session
          .getTransport(protocol);
      if (auth) {
        t.connect(serverUrl, serverUsername, serverPassword);
      } else {
        t.connect();
      }
      t.sendMessage(mail, mail.getAllRecipients());
      return true;
    } catch (Exception e) {
      Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_ERROR,
          International.getString("email-Versand fehlgeschlagen") + ": " + e.getMessage());
      Logger.logdebug(e);
      return false;
    }
  }

  private boolean sendMail(MultiPartMessage msg) {
    try {
      StringBuilder recipients = new StringBuilder();
      for (int i = 0; i < msg.addresses.size(); i++) {
        recipients.append((recipients.length() > 0 ? ", " : "") + msg.addresses.get(i));
      }
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 3)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL,
            "Trying to send multipart message to " + recipients.toString() + " ...");
      }
      boolean auth = (serverUsername != null && serverPassword != null);
      String protocol = Daten.efaConfig.getValueEmailSSL() ? "smtps" : "smtp";
      Properties props = new Properties();
      props.put("mail." + protocol + ".host", serverUrl);
      props.put("mail." + protocol + ".port", serverPort);
      if (auth) {
        props.put("mail." + protocol + ".auth", "true");
      }
      if (Daten.efaConfig.getValueEmailSSL()) {
        props.put("mail." + protocol + ".ssl.enable", "true");
      } else {
        props.put("mail." + protocol + ".starttls.enable", "true");
      }
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
        props.put("mail.debug", "true");
      }
      MailAuthenticator ma = null;
      if (auth) {
        ma = new MailAuthenticator(serverUsername, serverPassword);
      }
      String charset = Daten.ENCODING_ISO;
      javax.mail.Session session = javax.mail.Session.getInstance(props, ma);
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
        session.setDebugOut(Logger.getPrintStream());
      }
      com.sun.mail.smtp.SMTPMessage mail = new com.sun.mail.smtp.SMTPMessage(session);
      mail.setAllow8bitMIME(true);
      mail.setHeader("X-Mailer", Daten.EFA_SHORTNAME + " " + Daten.VERSIONID);
      mail.setHeader("Content-Type", "text/plain; charset=" + charset);
      mail.setFrom(new javax.mail.internet.InternetAddress(mailFromName + " <" + mailFromEmail
          + ">"));
      mail.setRecipients(RecipientType.TO,
          javax.mail.internet.InternetAddress.parse(recipients.toString()));
      mail.setSubject((mailSubjectPrefix != null ? "[" + mailSubjectPrefix + "] " : "")
          + msg.subject, charset);
      mail.setSentDate(new Date());
      mail.setContent(msg.message);
      com.sun.mail.smtp.SMTPTransport t = (com.sun.mail.smtp.SMTPTransport) session
          .getTransport(protocol);
      if (auth) {
        t.connect(serverUrl, serverUsername, serverPassword);
      } else {
        t.connect();
      }
      t.sendMessage(mail, mail.getAllRecipients());
      if (msg.deleteAttachmentFiles) {
        for (int i = 0; msg.attachmentFileNames != null
            && i < msg.attachmentFileNames.length; i++) {
          EfaUtil.deleteFile(msg.attachmentFileNames[i]);
        }
      }
      Logger.log(Logger.INFO, Logger.MSG_CORE_MAILSENT,
          LogString.emailSuccessfullySend(msg.subject));
      return true;
    } catch (Exception e) {
      Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_ERROR,
          International.getString("email-Versand fehlgeschlagen") + ": " +
              e.toString() + " " + e.getMessage());
      Logger.logdebug(e);
      return false;
    }
  }

  @Override
  public void run() {
    int errorCount = 0;
    while (true) {
      try {
        if (Daten.efaConfig != null && Daten.project != null && Daten.admins != null &&
            Daten.efaConfig.isOpen() && Daten.project.isOpen() && Daten.admins.isOpen() &&
            !Daten.project.isInOpeningProject()) {

          updateMailProperties();
          updateAdminEmailAddresses();

          if (Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
            int countToBeSent = 0;
            int countSuccess = 0;
            Messages messages = Daten.project.getMessages(false);
            if (messages == null || messages.data() == null
                || messages.data().getStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
              continue; // EmailSenderThread must only run for local messages!
            }
            DataKeyIterator it = messages.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
              MessageRecord msg = (MessageRecord) messages.data().get(k);
              if (msg != null && msg.getToBeMailed()) {
                // new message found
                countToBeSent++;
                boolean markDone = false;
                if (emailAddressesAdmin != null || emailAddressesBoatMaintenance != null) {
                  // recipient email addresses configured
                  if (serverUrl != null && serverPort != null
                      && mailFromEmail != null && mailFromName != null) {
                    // server properly configured
                    if (MessageRecord.TO_ADMIN.equals(msg.getTo()) && emailAddressesAdmin != null) {
                      Vector<String> emailListe = new Vector<String>();
                      emailListe.addAll(emailAddressesAdmin);
                      if (isValidEmailAdress(msg.getReplyTo())) {
                        emailListe.add(msg.getReplyTo());
                      }
                      markDone = sendMail(msg, emailListe);
                      if (markDone) {
                        countSuccess++;
                      }
                    }
                    if (MessageRecord.TO_BOATMAINTENANCE.equals(msg.getTo())
                        && emailAddressesBoatMaintenance != null) {
                      markDone = sendMail(msg, emailAddressesBoatMaintenance);
                      if (markDone) {
                        countSuccess++;
                      }
                    }
                    if (isValidEmailAdress(msg.getTo())) {
                      Vector<String> emailListe = new Vector<String>();
                      emailListe.add(msg.getTo());
                      markDone = sendMail(msg, emailListe);
                      if (markDone) {
                        countSuccess++;
                      }
                    }
                  } else {
                    Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                        International.getString("Kein email-Versand möglich!") + " "
                            + International.getString("Mail-Konfiguration unvollständig."));
                  }
                } else {
                  markDone = true; // no email recipients configured - mark this message as done
                }
                if (markDone) {
                  msg.setToBeMailed(false);
                  messages.data().update(msg);
                  errorCount = 0;
                } else {
                  if (errorCount < 10) {
                    errorCount++;
                    break;
                  }
                }
              }
              k = it.getNext();
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND)) {
              Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL, "EmailSenderThread: "
                  + countToBeSent + " unsent messages found; " + countSuccess
                  + " messages successfully sent.");
            }
          }

          for (int i = 0; i < multipartMessages.size(); i++) {
            if (sendMail(multipartMessages.get(i))) {
              multipartMessages.remove(i--);
            }
          }
        }

        Thread.sleep((1 + (errorCount * 10)) * 60 * 1000);
      } catch (Exception e) {
        Logger.logdebug(e);
      }
    }
  }

  private boolean isValidEmailAdress(String candidate) {
    if (candidate == null) {
      return false;
    }
    if (candidate.isEmpty()) {
      return false;
    }
    if (!candidate.contains("@")) {
      return false;
    }
    return true;
  }

  class MailAuthenticator extends javax.mail.Authenticator {

    String username;
    String password;

    public MailAuthenticator(String username, String password) {
      this.username = username;
      this.password = password;
    }

    @Override
    public javax.mail.PasswordAuthentication getPasswordAuthentication() {
      return new javax.mail.PasswordAuthentication(username, password);
    }
  }

  class MultiPartMessage {

    javax.mail.Multipart message;
    Vector<String> addresses;
    String subject;
    String[] attachmentFileNames;
    boolean deleteAttachmentFiles;

    public MultiPartMessage(javax.mail.Multipart message,
        Vector<String> addresses, String subject,
        String[] attachmentFileNames, boolean deleteAttachmentFiles) {
      this.message = message;
      this.addresses = addresses;
      this.subject = subject;
      this.attachmentFileNames = attachmentFileNames;
      this.deleteAttachmentFiles = deleteAttachmentFiles;
    }
  }
}
