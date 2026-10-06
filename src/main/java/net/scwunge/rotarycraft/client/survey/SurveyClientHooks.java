package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.Minecraft;
import net.scwunge.rotarycraft.survey.RadarMenu;
import net.scwunge.rotarycraft.survey.SurveyNetwork;

/** Client side of the survey machines' packets. Only called from handlers that run on the client. */
public final class SurveyClientHooks {
    private SurveyClientHooks() {}

    public static void radarData(SurveyNetwork.RadarData data) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof RadarMenu menu
                && menu.containerId == data.containerId()) {
            menu.setScan(data.scan());
        }
    }

    public static void gprData(SurveyNetwork.GprData data) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof net.scwunge.rotarycraft.survey.GprMenu menu
                && menu.containerId == data.containerId()) {
            menu.setData(data.range(), data.centre(), data.palette(), data.columns());
        }
    }

    public static void spyCamData(SurveyNetwork.SpyCamData data) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof net.scwunge.rotarycraft.survey.SpyCamMenu menu
                && menu.containerId == data.containerId()) {
            menu.setView(data.colors(), data.mobs());
        }
    }

    public static void viewCamera(net.minecraft.core.BlockPos pos) {
        CameraView.start(pos);
    }
}
