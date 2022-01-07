package launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Calendar;

import constants.programs.GarbageDataBaseRemover;
import constants.programs.EquipRemover;
import constants.programs.ControlUnit;

import server.maps.MapleMapObjectHandler;
import database.MYSQL;

import client.MapleCharacter;
import client.skills.SkillStatEffectCancelHandler;
import client.skills.VCoreFactory;
import constants.subclasses.setScriptableNPC;
import constants.programs.RewardScroll;
import constants.programs.HighRanking;
import constants.subclasses.QuickMove;
import constants.ServerConstants;
import constants.DemianPattern;
import constants.GameConstants;
import constants.DeathCount;
//파이널HP 
//import constants.FinalMaxHpConstants;

import handler.auction.AuctionHandler.WorldAuction;
import launch.helpers.MapleCacheData;

import packet.chat.opcode.ChatRecvPacketOpcode;
import packet.opcode.RecvPacketOpcode;
import packet.opcode.SendPacketOpcode;
import server.items.CashItemFactory;
import server.life.MapleMonsterProvider;
import server.life.ButterFly;
import server.named.Named;

import tools.MemoryUsageWatcher;
import tools.Timer.WorldTimer;
import tools.Pair;

import java.net.InetAddress;
import server.life.MapleLifeProvider;
import server.life.MapleMonster;

public final class Start {

    public static long START = System.currentTimeMillis();
    public static String createChar = "";

    public static void main(String args[]) throws IOException, InterruptedException {
        System.out.println("[알림] Server Name         = " + ServerConstants.serverName);
        System.out.println("[알림] Server Version      = KMS 1.2." + ServerConstants.MAPLE_VERSION + "[" + ServerConstants.subVersion + "]");
        InetAddress local = InetAddress.getLocalHost();
        System.out.println("[알림] Server IP           = " + local.getHostAddress() + "\r\n");
        MYSQL.init();

        try {
            Connection con = MYSQL.getConnection();
            PreparedStatement ps = con.prepareStatement("SET GLOBAL max_allowed_packet = 1073741824;");
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("SET GLOBAL max_connections = 20000;");
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("show variables where Variable_name = 'max_allowed_packet';");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
            }
            rs.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        /* 타이머 시작 */
        tools.Timer.WorldTimer.getInstance().start();
        tools.Timer.EtcTimer.getInstance().start();
        tools.Timer.MapTimer.getInstance().start();
        tools.Timer.CloneTimer.getInstance().start();
        tools.Timer.EventTimer.getInstance().start();
        tools.Timer.BuffTimer.getInstance().start();
        tools.Timer.PingTimer.getInstance().start();
        tools.Timer.ShowTimer.getInstance().start();

        /* EquipRemover 가동 */
        GarbageDataBaseRemover.main(args);
        EquipRemover.main(args);

        /* 데이터베이스 정리 */
        try {
            Connection con = MYSQL.getConnection();
            PreparedStatement del = con.prepareStatement("DELETE FROM acceptip");
            del.executeUpdate();
            del.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        /* 소켓 설정 및 서버 가동 */
        LoginServer.getInstance().run_startup_configurations();
        ChannelServer.startServer();
        CashShopServer.getInstance().run_startup_configurations();
        AuctionServer.getInstance().run_startup_configurations();
        BuddyChatServer.getInstance().run_startup_configurations();
        AdminToolServer.run_startup_configurations();
        
        /* 옵코드 설정 */
        SendPacketOpcode.loadOpcode();
        RecvPacketOpcode.loadOpcode();
        ChatRecvPacketOpcode.initalized();

        /* 캐싱쓰레드 시작 */
		//GameConstants.dList.add(new Pair("mirrorD_322_0_", "귀엽고 예쁜 치장 아이템, 메신저 아이템, 헤어샵 이용쿠폰, 성형외과 이용쿠폰 등 다양한 아이템을 살 수 있는 곳"));
		//GameConstants.dList.add(new Pair("mirrorD_322_1_", "각종 맵으로 이동할 수 있다."));
		//GameConstants.dList.add(new Pair("mirrorD_322_2_", "각종 장비,소비,코인,큐브 상점을 이용할수있습니다."));
		//GameConstants.dList.add(new Pair("mirrorD_322_3_", "환생 시스템을 이용할 수 있다."));
		//GameConstants.dList.add(new Pair("mirrorD_323_0_", "그랜드스토리에서 이용할 수 있는 다양한 컨텐츠들을 한눈에 확인할 수 있다."));
		//GameConstants.dList.add(new Pair("mirrorD_323_1_", "각종 장비, 아이템들을 강화/제작할 수 있다."));
        CashItemFactory.getInstance();
        Start.clean();
        MapleCacheData mc = new MapleCacheData();
        mc.startCacheData();
        HighRanking.getInstance().startTasking();
        WorldAuction.load();
        QuickMove.doMain();
        setScriptableNPC.doMain();
        RewardScroll.getInstance();
        MapleMonsterProvider.getInstance().retrieveGlobal();
        VCoreFactory.LodeCore();
        //Rank1Character(); //전광판 랭킹
        //LoginPoint1Character(); //로그인 포인트 랭킹
        //cRank1Character(); //추천인 랭킹
       // Pop1Character(); //인기도 랭킹
        //Meso1Character(); //메소 랭킹
        catchHair_Face(); //헤어,성형
        MChat_Chr();
        ButterFly.load();
        DemianPattern.initDemianPattern();
        DeathCount.main(args);
//파이날HP updateFinalMaxHp();
        WorldTimer.getInstance().register(new SkillStatEffectCancelHandler(), 1000L);
        WorldTimer.getInstance().register(new MapleMapObjectHandler(), 1000L);
        BufferedReader br;
        
        try (FileReader fl = new FileReader("Wz/Etc.wz/MakeCharInfo.img.xml")) {
            br = new BufferedReader(fl);
            String readLine = null;
            while ((readLine = br.readLine()) != null) {
                createChar += readLine;
            }
            br.close();
        }

        if (ServerConstants.AutoHotTimeSystem) {
            AutoHotTimeItem.main(args);
            System.out.println("[알림] " + ServerConstants.AutoHotTimeSystemHour + "시 " + ServerConstants.AutoHotTimeSystemMinute + "분에 자동으로 핫타임 아이템을 지급합니다.");
        }
        long END = System.currentTimeMillis();
        System.out.println("[알림] 서버 오픈이 정상적으로 완료 되었으며, 소요된 시간은 : " + (END - START) / 1000.0 + "초 입니다.");
        ControlUnit.main(args);
        MemoryUsageWatcher.main(args);
        Named.main(args);
    }

    public static void clean() {
        try {
            int nu = 0;
            PreparedStatement ps;
            Calendar ocal = Calendar.getInstance();
            Connection con = MYSQL.getConnection();
            ps = con.prepareStatement("SELECT * FROM acheck WHERE day = 1");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String key = rs.getString("keya");
                String day = ocal.get(ocal.YEAR) + "" + (ocal.get(ocal.MONTH) + 1) + "" + ocal.get(ocal.DAY_OF_MONTH);
                String da[] = key.split("_");
                if (!da[0].equals(day)) {
                    ps = con.prepareStatement("DELETE FROM acheck WHERE keya = ?");
                    ps.setString(1, key);
                    ps.executeUpdate();
                    nu++;
                }
            }
            rs.close();
            ps.close();
            con.close();
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /*public static void Rank1Character() {
        try {
            Connection con = MYSQL.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE gm = 0 ORDER BY reborns DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ServerConstants.chr = MapleCharacter.loadCharFromDB(rs.getInt("id"), null, false);
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("[알림] 전광판에 등록할 랭킹을 불러 왔습니다.");
    }*/

   /* public static void LoginPoint1Character() {
        try {
            Connection con = MYSQL.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE gm = 0 ORDER BY nxCash DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ServerConstants.loginPointAid = rs.getInt("id");
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("[알림] 로그인포인트 1위 랭킹을 불러 왔습니다.");
    }*/
    public static void MChat_Chr() {
        try {
            Connection con = MYSQL.getConnection();
            ResultSet sql = con.prepareStatement("SELECT * FROM characters WHERE gm = 0 ORDER BY fame DESC LIMIT 2").executeQuery();
            while (sql.next()) {
                ServerConstants.mChat_char.add(MapleCharacter.loadCharFromDB(sql.getInt("id"), null, false));
            }
            sql.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /*public static void Meso1Character() {
        try {
            Connection con = MYSQL.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE gm = 0 ORDER BY meso DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ServerConstants.mrank1 = rs.getString("name");
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("[알림] 메소1위 랭킹을 불러 왔습니다.");
    }*/

    /*public static void Pop1Character() {
        try {
            Connection con = MYSQL.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE gm = 0 ORDER BY fame DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ServerConstants.prank1 = rs.getString("name");
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("[알림] 인기도1위 랭킹을 불러 왔습니다.");
    }*/

    /*public static void cRank1Character() {
        try {
            Connection con = MYSQL.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT id, recom, count(*) AS player FROM recom_log GROUP BY recom ORDER BY player DESC")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ServerConstants.crank1 = rs.getString("recom");
                }
                rs.close();
                ps.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("[알림] 추천인1위 랭킹을 불러 왔습니다.");
    }*/
    /*파이날HP
        public static final void updateFinalMaxHp() {
        for (int i = 0; i < FinalMaxHpConstants.finalMonsterMaxHpMobCode.size(); i++) {
            try {
                MapleMonster monster = MapleLifeProvider.getMonster(FinalMaxHpConstants.finalMonsterMaxHpMobCode.get(i));
                System.out.println("[알림] 보스 [" + monster.getStats().getName() + "]  [체력 : " + FinalMaxHpConstants.finalMonsterMaxHp.get(i) + "]");
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }
*/

    public static void catchHair_Face() {
        File Hair = new File("wz/Character.wz/Hair");
        File Face = new File("wz/Character.wz/Face");
        for (File file : Hair.listFiles()) {
            ServerConstants.real_face_hair += file.getName();
        }
        for (File file : Face.listFiles()) {
            ServerConstants.real_face_hair += file.getName();
        }
        System.out.println("[알림] 헤어 및 성형코드를 캐싱 완료 하였습니다. \r\n");
    }
}
