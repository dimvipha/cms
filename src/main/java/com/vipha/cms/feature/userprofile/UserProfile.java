package com.vipha.cms.feature.userprofile;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor

@Entity //for map table
@Table(name="user_profile")
public class UserProfile {

    @Id
    private String id; //keycloak user id

    @Column(length = 6)
    private String gender;

    private String biography;

    private String profilePicture;

    private String facebookProfile;

    private String telegramProfile;



}