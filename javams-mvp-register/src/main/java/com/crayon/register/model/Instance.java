package com.crayon.register.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务实例对象，用于对外提供通信方式或其他关键信息
 *
 * <p>
 * 基本信息有：IP、端口、标识、状态
 * </p>
 *
 * @author crayon
 * @version 1.0
 * @date 2025/4/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Instance {

    /**
     * 实例id
     * <p>
     * 使用整数类型，相较于字符串类型，占用空间小，且可排序。
     * 而且服务应该不会意外耗尽整数，所以使用整数类型。
     */
    private Integer id;

    /**
     * 这里是方便传递，实际中，直接存储字符串类型不是很好。
     * ip可以使用无符号整数来持久化。MySQL数据库提供有INET_ATON和INET_NTOA函数来转换。
     */
    private String ip;

    /**
     * 端口号
     *
     */
    private Integer port;


    /**
     * 服务名称
     * 用于标识服务
     */
    private String serverName;

    /**
     * 持久化数据使用tinyint 存储
     * 用枚举表达方便维护、扩展
     */
    private Status status;

    /**
     * 上次心跳时间
     */
    private Long lastHeartbeat;

    /**
     * 健康状态
     * <p>
     * 用于实现健康阈值保护逻辑
     */
    private boolean healthy;

    public Instance(String ip, int port, boolean healthy) {
        this.ip = ip;
        this.port = port;
        this.healthy = healthy;
    }

    public enum Status {
        ONLINE(1),
        OFFLINE(0);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        // 根据 code 获取枚举
        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.getCode() == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }

    }

}
