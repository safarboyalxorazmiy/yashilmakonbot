package uz.jarvis.user.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "bot_user_history", schema = "telegram_bot")
public class UserHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  @Enumerated(value = EnumType.STRING)
  private Label label;

  @Column
  private Long userId;

  @Column
  private String value;
}