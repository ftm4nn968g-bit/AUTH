package kz.onlineshop.auth.service;

/** Тіркелу кезіндегі қате (email бос емес, құпия сөз әлсіз т.б.). Хабарлама пайдаланушыға көрсетіледі. */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
        super(message);
    }
}
