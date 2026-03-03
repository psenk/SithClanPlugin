package com.sithclanplugin;

import com.sithclanplugin.SithClanPlugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SithClanPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SithClanPlugin.class);
		RuneLite.main(args);
	}
}