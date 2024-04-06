package uz.jarvis.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor

@Entity
@Table(name = "bot_user", schema = "telegram_bot")
public class UserEntity {
  @Id
  private Long chatId;

  @Column
  private String firstName;

  @Column
  private String lastName;

  @Column
  private LocalDateTime registerAt;

  @Enumerated(value = EnumType.STRING)
  @Column
  private Role role;
}