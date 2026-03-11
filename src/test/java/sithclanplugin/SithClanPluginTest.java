package sithclanplugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import sithclanplugin.SithClanPlugin;

public class SithClanPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SithClanPlugin.class);
		RuneLite.main(args);
	}
}