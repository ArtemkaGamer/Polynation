package com.polynation.server.dto.response;

import com.polynation.server.model.User;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private Integer rating;
    private Integer quizzesSolved;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.email = u.getEmail();
        r.role = u.getRole();
        r.createdAt = u.getCreatedAt();
        r.rating = u.getRating();
        r.quizzesSolved = u.getQuizzesSolved();
        return r;
    }
}
