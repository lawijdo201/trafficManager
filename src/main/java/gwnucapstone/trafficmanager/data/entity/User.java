package gwnucapstone.trafficmanager.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity//Entity 설정
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String pw;
    private String name;
    private String email;
}
