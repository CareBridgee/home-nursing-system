package iti.jets.java.homenursing.service;

public interface GoogleTokenVerifier {

    record GoogleUserInfo(String googleSub, String email, String givenName,
                          String familyName, String picture) {
    }

    GoogleUserInfo verify(String idToken);
}
