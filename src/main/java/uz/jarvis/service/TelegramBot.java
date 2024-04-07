package uz.jarvis.service;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.jarvis.config.BotConfig;
import uz.jarvis.user.Role;
import uz.jarvis.user.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.jarvis.user.history.Label;
import uz.jarvis.user.history.UserHistoryService;

import org.telegram.telegrambots.meta.api.methods.GetFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import java.util.ArrayList;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
  private final BotConfig config;
  private final UsersService usersService;
  private final UserHistoryService userHistoryService;

  public TelegramBot(
    BotConfig config,
    UsersService usersService,
    UserHistoryService userHistoryService
  ) {
    this.config = config;
    this.usersService = usersService;
    this.userHistoryService = userHistoryService;

    List<BotCommand> listOfCommands = new ArrayList<>();
    listOfCommands.add(new BotCommand("/start", "Boshlash"));

    try {
      this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
    } catch (TelegramApiException e) {
      log.error("Error during setting bot's command list: {}", e.getMessage());
    }
  }

  @Override
  public String getBotUsername() {
    return config.getBotName();
  }

  @Override
  public String getBotToken() {
    return config.getToken();
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      long chatId = update.getMessage().getChatId();
      if (update.getMessage().getChat().getType().equals("supergroup")) {
        // DO NOTHING CHANNEL CHAT ID IS -1001764816733
        return;
      } else {
        Role role = usersService.getRoleByChatId(chatId);

        if (update.hasMessage() && update.getMessage().hasText()) {
          String messageText = update.getMessage().getText();

          if (messageText.startsWith("/")) {
            if (messageText.startsWith("/login ")) {
              String password = messageText.substring(7);

              if (password.equals("Xp2s5v8y/B?E(H+KbPeShVmYq3t6w9z$C&F)J@NcQfTjWnZr4u7x!A%D*G-KaPdSgUkXp2s5v8y/B?E(H+MbQeThWmYq3t6w9z$C&F)J@NcRfUjXn2r4u7x!A%D*G-Ka")) {
                usersService.changeRole(chatId, Role.ROLE_AGENT);
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                return;
              } else if (password.equals("674935b4fa5e4641853a42c43100de99")) {
                usersService.changeRole(chatId, Role.ROLE_OWNER);
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                return;
              }
              return;
            }

            switch (messageText) {
              case "/start" -> {
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                return;
              }
              default -> {
                sendMessage(chatId, "Sorry, command was not recognized");
                return;
              }
            }
          }


          if (role.equals(Role.ROLE_AGENT)) {
            User user = update.getMessage().getFrom();
            if (messageText.equals("Daraxt qo'shish ➕")) {
              sendMessageWithKeyboardButtons(
                chatId,
                "Ism va familyangizni kiriting: ",
                List.of(user.getLastName() == null ? user.getFirstName() : user.getFirstName() + " " + user.getLastName())
              );

              userHistoryService.clearHistory(chatId);
              userHistoryService.create(Label.OFFER_STARTED, chatId, "NO_VALUE");
            } else if (messageText.equals("\uD83D\uDD19 Bosh menyuga qaytish")) {
              sendMessageWithKeyboardButtons(chatId, "Bosh menyu \uD83C\uDFD8", List.of("Plan \uD83D\uDCED", "Hisobot \uD83D\uDCDD"));
            } else if (messageText.equals("Bekor qilish \uD83D\uDD19")) {
              sendMessageWithKeyboardButtons(chatId, "Bosh menyu \uD83C\uDFD8", List.of("Plan \uD83D\uDCED", "Hisobot \uD83D\uDCDD"));
              userHistoryService.clearHistory(chatId);
            }


            // STEP CONTROL
            else {
              Label lastLabelByChatId = userHistoryService.getLastLabelByChatId(chatId);
              if (lastLabelByChatId != null) {
                if (lastLabelByChatId.equals(Label.OFFER_STARTED)) {

                  userHistoryService.create(Label.FULL_NAME_ASKED, chatId, messageText);

                  List<String> models = new ArrayList<>();
                  models.add("Sharq chinori");
                  models.add("Shumbargli zarang");
                  models.add("Oddiy shumtol");
                  models.add("Bekor qilish \uD83D\uDD19");
                  sendMessageWithKeyboardButtons(chatId, "<b>Daraxt turini tanlang:</b>", models);

                  userHistoryService.create(Label.TYPE_ASKING, chatId, "NO_VALUE");
                } else if (lastLabelByChatId.equals(Label.TYPE_ASKING)) {
                  userHistoryService.create(Label.TYPE_ASKED, chatId, messageText);

                  SendMessage sendMessage = new SendMessage();
                  sendMessage.setText("Joylashuvni kiriting: ");
                  sendMessage.setChatId(chatId);

                  ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                  keyboardMarkup.setOneTimeKeyboard(true);
                  keyboardMarkup.setResizeKeyboard(true);

                  KeyboardButton button = new KeyboardButton();
                  button.setText("Share Location");
                  button.setRequestLocation(true);

                  KeyboardRow row = new KeyboardRow();
                  row.add(button);

                  List<KeyboardRow> keyboard = new ArrayList<>();
                  keyboard.add(row);

                  keyboardMarkup.setKeyboard(keyboard);

                  sendMessage.setReplyMarkup(keyboardMarkup);

                  try {
                    execute(sendMessage);
                    userHistoryService.create(Label.TREE_LOCATION_ASKING, chatId, "NO_VALUE");
                  } catch (TelegramApiException ignored) {
                  }
                }
              }
            }
          } else if (role.equals(Role.ROLE_USER)) {
          } else if (role.equals(Role.ROLE_OWNER)) {
          }
        } else if (update.hasMessage() && update.getMessage().hasLocation()) {
          Label lastLabelByChatId = userHistoryService.getLastLabelByChatId(chatId);
          if (lastLabelByChatId.equals(Label.TREE_LOCATION_ASKING)) {
            Location location = update.getMessage().getLocation();
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            userHistoryService.create(Label.LOCATION_LATITUDE, chatId, String.valueOf(latitude));
            userHistoryService.create(Label.LOCATION_LONGITUDE, chatId, String.valueOf(longitude));

            sendMessage(chatId, "<b>Daraxt rasmini jo'nating.</b>");

            userHistoryService.create(Label.TREE_PHOTO_ASKING, chatId, String.valueOf(longitude));
          }
        }

        if (update.hasMessage() && update.getMessage().hasPhoto()) {
          Label lastLabelByChatId = userHistoryService.getLastLabelByChatId(chatId);

          if (lastLabelByChatId.equals(Label.TREE_PHOTO_ASKING)) {
            sendMessageWithKeyboardButtons(
              chatId,
              "<b>Daraxt muvafaqqiyatli yaratildi!</b>",
              List.of("Daraxt qo'shish ➕", "Daraxtlar haqida ma'lumot \uD83C\uDF33")
            );

            List<PhotoSize> photos = update.getMessage().getPhoto();
            // Sort photos by file size, to get the largest one
            photos.sort(Comparator.comparing(PhotoSize::getFileSize).reversed());

            PhotoSize photo = photos.get(0); // Get the highest resolution photo
            String fileId = photo.getFileId();

            try {
              java.io.File localFile = downloadPhoto(fileId);
              System.out.println("Photo saved to: " + localFile.getAbsolutePath());

              String photoId = ApiService.uploadImage(localFile.getAbsolutePath());

              String fullName = userHistoryService.getLastValueByChatId(chatId, Label.FULL_NAME_ASKED);
              String type = userHistoryService.getLastValueByChatId(chatId, Label.TYPE_ASKED);
              String latitude = userHistoryService.getLastValueByChatId(chatId, Label.LOCATION_LATITUDE);
              String longitude = userHistoryService.getLastValueByChatId(chatId, Label.LOCATION_LONGITUDE);

              // SAVE DATA VIA API THEN SEND SUCCESSFULLY SAVED RESPONSE
              String path = ApiService.createTree(fullName, type, latitude, longitude, photoId);

              // Send this photo with java telegram bot SendPhoto the path in the top
              SendPhoto sendPhoto = new SendPhoto();
              sendPhoto.setChatId(chatId);
              sendPhoto.setPhoto(new InputFile(new File(path)));
              try {
                execute(sendPhoto);
              } catch (TelegramApiException ignored) {

              }



            } catch (TelegramApiException e) {
              throw new RuntimeException(e);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    } else if (update.hasCallbackQuery()) {
    }
  }

  private java.io.File downloadPhoto(String fileId) throws IOException, TelegramApiException {
    // Get file path from Telegram API
    GetFile getFile = new GetFile();
    getFile.setFileId(fileId);
    org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
    java.io.File localFile = java.io.File.createTempFile(UUID.randomUUID().toString(), ".jpg");
    try (FileOutputStream fos = new FileOutputStream(localFile)) {
      // Download photo to local file
      downloadFileAsStream(file.getFilePath(), fos);
    }
    return localFile;
  }

  private void downloadFileAsStream(String filePath, FileOutputStream fos) throws IOException {
    java.net.URL url = new java.net.URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath);
    try (java.io.InputStream is = url.openStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        fos.write(buffer, 0, bytesRead);
      }
    }
  }

  private void startCommandReceived(long chatId, String firstName, String lastName) {
    Role role = usersService.createUser(chatId, firstName, lastName).getRole();

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.enableHtml(true);

    if (role.equals(Role.ROLE_USER)) {
      message.setText("Welcome User, What's up?");
    } else if (role.equals(Role.ROLE_AGENT)) {
      message.setText("Hush kelibsiz, Agent!");

      ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
      List<KeyboardRow> rows = new ArrayList<>();
      KeyboardRow row = new KeyboardRow();
      row.add("Daraxt qo'shish ➕");
      row.add("Daraxtlar haqida ma'lumot \uD83C\uDF33");
      rows.add(row);
      replyKeyboardMarkup.setResizeKeyboard(true);
      replyKeyboardMarkup.setKeyboard(rows);

      message.setReplyMarkup(replyKeyboardMarkup);
    } else if (role.equals(Role.ROLE_OWNER)) {
      message.setText("Hush kelibsiz, Asoschi!");

      ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
      List<KeyboardRow> rows = new ArrayList<>();
      KeyboardRow row = new KeyboardRow();
      row.add("Mahsulot qo'shish ➕");
      rows.add(row);
      replyKeyboardMarkup.setResizeKeyboard(true);
      replyKeyboardMarkup.setKeyboard(rows);

      message.setReplyMarkup(replyKeyboardMarkup);
    }

    try {
      execute(message);
    } catch (TelegramApiException e) {
      log.error("Error in startCommandReceived()");
    }
  }

  private void sendMessage(long chatId, String textToSend) {
    SendMessage message = new SendMessage();

    message.setChatId(chatId);
    message.setText(textToSend);
    message.enableHtml(true);
    try {
      execute(message);
    } catch (TelegramApiException ignored) {
      log.error("Error in sendMessage()");
    }
  }

  private void sendMessageWithKeyboardButtons(long chatId, String textToSend, List<String> keyboardRowText) {
    SendMessage message = new SendMessage();

    message.setChatId(chatId);
    message.setText(textToSend);
    message.enableHtml(true);

    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    List<KeyboardRow> rows = new ArrayList<>();
    if (keyboardRowText.size() < 3) {
      KeyboardRow row = new KeyboardRow();

      for (String s : keyboardRowText) {
        row.add(s);
      }

      rows.add(row);
    } else {
      for (String s : keyboardRowText) {
        KeyboardRow row = new KeyboardRow();
        row.add(s);
        rows.add(row);
      }
    }

    replyKeyboardMarkup.setResizeKeyboard(true);
    replyKeyboardMarkup.setKeyboard(rows);

    message.setReplyMarkup(replyKeyboardMarkup);

    try {
      execute(message);
    } catch (TelegramApiException ignored) {
      log.error("Error in sendMessageWithKeyboardButton()");
    }
  }

  private void sendMessageWithKeyboardButton(long chatId, String textToSend, String keyboardRowText) {
    SendMessage message = new SendMessage();

    message.setChatId(chatId);
    message.setText(textToSend);
    message.enableHtml(true);

    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    List<KeyboardRow> rows = new ArrayList<>();

    KeyboardRow row = new KeyboardRow();
    row.add(keyboardRowText);
    rows.add(row);


    replyKeyboardMarkup.setResizeKeyboard(true);
    replyKeyboardMarkup.setKeyboard(rows);

    message.setReplyMarkup(replyKeyboardMarkup);

    try {
      execute(message);
    } catch (TelegramApiException ignored) {
      log.error("Error in sendMessageWithKeyboardButton()");
    }
  }

  public void deleteMessageById(Long chatId, Integer messageId) {
    try {
      DeleteMessage deleteMessage = new DeleteMessage();
      deleteMessage.setChatId(chatId);
      deleteMessage.setMessageId(messageId);

      execute(deleteMessage);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}