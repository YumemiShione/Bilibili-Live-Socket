package org.yumemi.bilibili.live.socket;

final class DataPacket{
    //全局单例的心跳数据包
    static final DataPacket HEARTBEAT_PACKET;
    //数据包中的协议版本的一些定义
    static final int PROTOCOL_JSON=0;
    static final int PROTOCOL_VIEWER_COUNT=1;
    static final int PROTOCOL_BUFFER=2;
    //数据包类型的一些定义
    static final int OPERATION_HEARTBEAT=2;//心跳包类型
    static final int OPERATION_HEARTBEAT_RESPONSE=3;//心跳包的成功响应类型
    static final int OPERATION_DATA=5;//数据包类型
    static final int OPERATION_HANDSHAKE=7;//协议握手(进房)包类型
    static final int OPERATION_HANDSHAKE_SUCCESS=8;//协议握手(进房)包的成功响应类型
    //数据包属性的一些常量定义
    static final int LENGTH_FIELD_LENGTH=4;//数据包包体大小属性占用的字节数
    static final short DEFAULT_HEADER_LENGTH=16;//默认的数据头占用的字节数
    static final short DEFAULT_PROTOCOL_VERSION=0;//默认使用(发送)的数据包协议的版本
    static final int DEFAULT_SEQUENCE_ID=1;//默认使用(发送)的数据包的序列号
    //数据包属性
    final int length;
    final short headerLength;
    final short protocolVersion;
    final int operation;
    final int sequenceId;
    final byte[] body;
    static{
        HEARTBEAT_PACKET=new DataPacket(OPERATION_HEARTBEAT,new byte[0]);
    }
    DataPacket(int length,short headerLength,short protocolVersion,int operation,int sequenceId,byte[] body){
        this.length=length;
        this.headerLength=headerLength;
        this.protocolVersion=protocolVersion;
        this.operation=operation;
        this.sequenceId=sequenceId;
        this.body=body;
    }
    private DataPacket(int operation,byte[] body){
        this(DEFAULT_HEADER_LENGTH+body.length,
                DEFAULT_HEADER_LENGTH,
                DEFAULT_PROTOCOL_VERSION,
                operation,
                DEFAULT_SEQUENCE_ID,
                body);
    }
    static DataPacket buildEnterRoomPacket(int roomId){
        //简易的构造JSON文本
        byte[] jsonBody=new StringBuilder().append('{')
                .append("\"roomid\":").append(roomId)
                .append('}').toString().getBytes();
        return new DataPacket(OPERATION_HANDSHAKE,jsonBody);
    }
}