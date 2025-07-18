package goodspace.backend.domain.user;

import goodspace.backend.domain.BaseEntity;
import goodspace.backend.domain.Order;
import goodspace.backend.security.RefreshToken;
import goodspace.backend.dto.UserMyPageDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class User extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private Integer dateOfBirth;
    @Column(unique = true, nullable = false)
    private String email;
    private String phoneNumber;

    @OneToMany(mappedBy = "user")
    private final List<Order> orders = new ArrayList<>();

    @Embedded
    private Delivery delivery;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    public void updateRefreshToken(String refreshTokenValue) {
        if (this.refreshToken == null) {
            this.refreshToken = new RefreshToken(refreshTokenValue);
            return;
        }

        this.refreshToken.setTokenValue(refreshTokenValue);
    }

    public String getRefreshToken() {
        if (refreshToken == null) {
            return "";
        }

        return this.refreshToken.getTokenValue();
    }

    public User setUserFromUserMyPageDto(UserMyPageDto userMyPageDto){
        this.email = userMyPageDto.getEmail();
        this.phoneNumber = userMyPageDto.getPhoneNumber();
        this.name = userMyPageDto.getName();
        this.dateOfBirth = userMyPageDto.getDateOfBirth();
        this.delivery = delivery.from(userMyPageDto);

        return this;
    }
}
