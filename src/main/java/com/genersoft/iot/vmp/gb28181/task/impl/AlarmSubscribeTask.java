package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * @author 20412
 */
public class AlarmSubscribeTask implements ISubscribeTask {
    private final Logger logger = LoggerFactory.getLogger(MobilePositionSubscribeTask.class);
    private Device device;
    private ISIPCommander sipCommander;

    private SIPRequest request;
    private DynamicTask dynamicTask;
    private String taskKey = "alarm-subscribe-timeout";

    public AlarmSubscribeTask(Device device, ISIPCommander sipCommander, DynamicTask dynamicTask) {
        this.device = device;
        this.sipCommander = sipCommander;
        this.dynamicTask = dynamicTask;
    }

    @Override
    public void run() {
        if (dynamicTask.get(taskKey) != null) {
            dynamicTask.stop(taskKey);
        }
        SIPRequest sipRequest = null;
        try {
            // 事件订阅 相当一部分设备不支持事件订阅 如海康。。。 这一部分通过布防处理 因为有部分设备不支持布防，如华为
            sipRequest = sipCommander.alarmSubscribe(device,1000,"0","9","0",null,null);
            logger.info("[事件订阅]成功： {}", device.getDeviceId());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 事件订阅: {}", e.getMessage());
        }
        if (sipRequest != null) {
            this.request = sipRequest;
        }

    }

    @Override
    public void stop(CommonCallback<Boolean> callback) {
        /**
         * dialog 的各个状态
         * EARLY-> Early state状态-初始请求发送以后，收到了一个临时响应消息
         * CONFIRMED-> Confirmed Dialog状态-已确认
         * COMPLETED-> Completed Dialog状态-已完成
         * TERMINATED-> Terminated Dialog状态-终止
         */
        if (dynamicTask.get(taskKey) != null) {
            dynamicTask.stop(taskKey);
        }
        try {
            // 当expires为0时取消订阅
            sipCommander.alarmSubscribe(device,0,"0","9","0",null,null);
            logger.info("[事件订阅]取消成功： {}", device.getDeviceId());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 事件订阅取消失败: {}", e.getMessage());
        }
        device.setSubscribeCycleForAlarm(0);
    }
}