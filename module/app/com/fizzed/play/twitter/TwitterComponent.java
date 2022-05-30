package com.fizzed.play.twitter;

import twitter4j.Status;

import java.util.List;

public interface TwitterComponent {
    List<Status> tweets();
    List<Status> tweetsJson();
}
