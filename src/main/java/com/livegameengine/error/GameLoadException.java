package com.livegameengine.error;

public class GameLoadException extends Exception {
	public GameLoadException(String message, Throwable inner) {
		super(message, inner);
	}
}
