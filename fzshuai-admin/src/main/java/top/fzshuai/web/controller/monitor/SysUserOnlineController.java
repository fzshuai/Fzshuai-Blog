package top.fzshuai.web.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import top.fzshuai.common.annotation.Log;
import top.fzshuai.common.constant.CacheConstants;
import top.fzshuai.common.core.controller.BaseController;
import top.fzshuai.common.core.domain.R;
import top.fzshuai.common.core.domain.dto.UserOnlineDto;
import top.fzshuai.common.core.page.TableDataInfo;
import top.fzshuai.common.enums.BusinessType;
import top.fzshuai.common.utils.StreamUtils;
import top.fzshuai.common.utils.StringUtils;
import top.fzshuai.common.utils.redis.RedisUtils;
import top.fzshuai.system.domain.SysUserOnline;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 在线用户监控
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController {

    /**
     * 获取在线用户监控列表
     *
     * @param ipaddr   IP地址
     * @param userName 用户名
     */
    @SaCheckPermission("monitor:online:list")
    @GetMapping("/list")
    public TableDataInfo<SysUserOnline> list(String ipaddr, String userName) {
        // 获取所有未过期的 token
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        List<UserOnlineDto> userOnlineDtoList = new ArrayList<>();
        for (String key : keys) {
            String token = StringUtils.substringAfterLast(key, ":");
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                continue;
            }
            userOnlineDtoList.add(RedisUtils.getCacheObject(CacheConstants.ONLINE_TOKEN_KEY + token));
        }
        if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName)) {
            userOnlineDtoList = StreamUtils.filter(userOnlineDtoList, userOnline ->
                StringUtils.equals(ipaddr, userOnline.getIpaddr()) &&
                    StringUtils.equals(userName, userOnline.getUserName())
            );
        } else if (StringUtils.isNotEmpty(ipaddr)) {
            userOnlineDtoList = StreamUtils.filter(userOnlineDtoList, userOnline ->
                StringUtils.equals(ipaddr, userOnline.getIpaddr())
            );
        } else if (StringUtils.isNotEmpty(userName)) {
            userOnlineDtoList = StreamUtils.filter(userOnlineDtoList, userOnline ->
                StringUtils.equals(userName, userOnline.getUserName())
            );
        }
        Collections.reverse(userOnlineDtoList);
        userOnlineDtoList.removeAll(Collections.singleton(null));
        List<SysUserOnline> userOnlineList = BeanUtil.copyToList(userOnlineDtoList, SysUserOnline.class);
        return TableDataInfo.build(userOnlineList);
    }

    /**
     * 强退用户
     *
     * @param tokenId token值
     */
    @SaCheckPermission("monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public R<Void> forceLogout(@PathVariable String tokenId) {
        try {
            StpUtil.kickoutByTokenValue(tokenId);
        } catch (NotLoginException ignored) {
        }
        return R.ok();
    }
}
