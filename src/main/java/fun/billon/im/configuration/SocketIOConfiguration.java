package fun.billon.im.configuration;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import fun.billon.auth.api.feign.IAuthService;
import fun.billon.auth.api.model.AuthOuterKeyModel;
import fun.billon.common.model.ResultModel;
import fun.billon.common.util.JwtUtils;
import fun.billon.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * socketio配置
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class SocketIOConfiguration {

    @Value("${billon.im.sid}")
    private String sid;

    @Value("${billon.im.host}")
    private String host;

    @Value("${billon.im.port}")
    private int port;

    @Resource
    private IAuthService authService;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setAuthorizationListener((handshakeData) -> auth(handshakeData));
        return new SocketIOServer(config);
    }

    /**
     * 必须配置该项，否则SocketIO的注解不生效
     */
    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new SpringAnnotationScanner(socketServer);
    }

    /**
     * 验证签名信息
     *
     * @param handshakeData 握手数据
     * @return 验签是否通过
     */
    private boolean auth(HandshakeData handshakeData) {
        String appId = handshakeData.getHttpHeaders().get("appId");
        if (StringUtils.isEmpty(appId)) {
            appId = handshakeData.getSingleUrlParam("appId");
        }
        String token = handshakeData.getHttpHeaders().get("token");
        if (StringUtils.isEmpty(token)) {
            token = handshakeData.getSingleUrlParam("token");
        }
        if (StringUtils.isEmpty(appId)
                || StringUtils.isEmpty(token)) {
            return false;
        }
        ResultModel<AuthOuterKeyModel> resultAuthOuterKeyModel = authService.outerKey(sid, sid, appId);
        if (resultAuthOuterKeyModel.getCode() == ResultModel.RESULT_SUCCESS
                && resultAuthOuterKeyModel.getData() != null) {
            AuthOuterKeyModel authOuterKeyModel = resultAuthOuterKeyModel.getData();
            /*
             * 获取到外部应用密钥，对token进行验证
             */
            try {
                DecodedJWT jwt = JwtUtils.verify(authOuterKeyModel.getAppId(), authOuterKeyModel.getAppSecret(),
                        authOuterKeyModel.getRefreshTokenExpTime(), token);
                // 将uid设置到handshakeData中
                handshakeData.getUrlParams().put("uid", Arrays.asList(jwt.getSubject()));
                return true;
            } catch (JWTVerificationException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

}