package sithclanplugin.util;

import com.google.inject.Singleton;

import lombok.Getter;
import lombok.Setter;

@Singleton
public class SithClanState
{
    @Getter
    @Setter
    private boolean isSenateMember = false;

    @Getter
    @Setter
    private String playerName = null;
}
