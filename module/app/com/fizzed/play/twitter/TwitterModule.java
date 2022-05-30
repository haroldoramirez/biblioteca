package com.fizzed.play.twitter;

import play.api.inject.Binding;
import play.api.inject.Module;

public class TwitterModule extends Module {

    @Override
    public scala.collection.Seq<Binding<?>> bindings(play.api.Environment environment, play.api.Configuration configuration) {
        return seq(
                bind(TwitterComponent.class).to(TwitterComponentImpl.class)
        );
    }
}
