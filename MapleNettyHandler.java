package launch.netty;

import static constants.subclasses.ServerType.AUCTION;
import static constants.subclasses.ServerType.CASHSHOP;
import static constants.subclasses.ServerType.CHANNEL;

import client.MapleClient;
import constants.ServerConstants;
import constants.subclasses.ServerType;
import handler.MapleServerHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import packet.creators.LoginPacket;
import packet.crypto.MapleCrypto;
import packet.opcode.RecvPacketOpcode;
import packet.transfer.read.ReadingMaple;
import tools.RandomStream.Randomizer;

public class MapleNettyHandler extends SimpleChannelInboundHandler<ReadingMaple> {

    private final ServerType serverType;
    private final int channel;

    public MapleNettyHandler(ServerType serverType, int channel) {
        this.serverType = serverType;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        final String address = ctx.channel().remoteAddress().toString().split(":")[0];
        switch (serverType) {
            case LOGIN:
                System.out.println("[알림] " + address + " 에서 로그인 서버로 연결을 시도했습니다.");
                break;
            case CHANNEL:
                //    System.out.println("[알림] " + address + " 에서 채널 서버로 연결을 시도했습니다.");
                break;
            case CASHSHOP:
                //    System.out.println("[알림] " + address + " 에서 캐시샵 서버로 연결을 시도했습니다.");
                break;
            case BUDDYCHAT:
                //    System.out.println("[알림] " + address + " 에서 채팅 서버로 연결을 시도했습니다.");
                break;
            case AUCTION:
                //    System.out.println("[알림] " + address + " 에서 경매장 서버로 연결을 시도했습니다.");
                break;
            default:
        }
        final byte serverRecv[] = {(byte) 0x22, (byte) 0x3F, (byte) 0x37, (byte) Randomizer.nextInt(255)};
        final byte serverSend[] = {(byte) 0xC9, (byte) 0x3A, (byte) 0x27, (byte) Randomizer.nextInt(255)};
        final byte ivRecv[] = serverRecv;
        final byte ivSend[] = serverSend;
        final MapleClient client = new MapleClient(ctx.channel(), new MapleCrypto(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION), serverType == CHANNEL || serverType == CASHSHOP || serverType == AUCTION, true), new MapleCrypto(ivRecv, ServerConstants.MAPLE_VERSION, serverType == CHANNEL || serverType == CASHSHOP || serverType == AUCTION));
        client.setChannel(channel);
        ctx.writeAndFlush(LoginPacket.initializeConnection(ServerConstants.MAPLE_VERSION, ivSend, ivRecv, !serverType.equals(ServerType.LOGIN)));
        ctx.channel().attr(MapleClient.CLIENTKEY).set(client);
        //client.sendPing();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();

        if (client != null) {
            //System.out.println(client.getIp() + " disconnected.");
            client.disconnect(true, false);
        }
        ctx.channel().attr(MapleClient.CLIENTKEY).set(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ReadingMaple slea) throws Exception {
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENTKEY).get();
        final short header_num = slea.readShort();
        short header_num1 = header_num;
        if (header_num1 != RecvPacketOpcode.MOVE_LIFE.getValue() && header_num1 != RecvPacketOpcode.MOVE_PLAYER.getValue() && header_num1 != RecvPacketOpcode.QUEST_ACTION.getValue() && header_num1 != RecvPacketOpcode.NPC_ACTION.getValue()) {
            //System.out.println("Recv " + header_num1 + " : " + RecvPacketOpcode.getOpcodeName(header_num1));
        }
        if (ServerConstants.showPackets == true /*&& header_num != RecvPacketOpcode.MOVE_LIFE.getValue()*/
                && header_num != RecvPacketOpcode.MOVE_PLAYER.getValue()
                && header_num != RecvPacketOpcode.QUEST_ACTION.getValue()
                && header_num != RecvPacketOpcode.NPC_ACTION.getValue()) {
            System.out.println("[" + header_num + "] " + slea.toString());
        }
        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
                try {
                    MapleServerHandler.handlePacket(recv, slea, c, serverType);
                } catch (Exception ex) {
                    //ex.printStackTrace(); //스킬 오류 구동기 표시
                }
                return;
            }
        }
    }
}
