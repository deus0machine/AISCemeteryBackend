package ru.cemeterysystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@Setter
@Getter
public class GuestDto {
    private Long id;
    private String fio;
    private String contacts;
    private Date dateOfRegistration;
    private String login;
    private Long balance;
}
