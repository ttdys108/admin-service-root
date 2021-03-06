package com.example.admin.data.service;

import com.example.admin.data.entity.ActionType;
import com.example.admin.data.entity.Menu;
import com.example.admin.data.mapper.MenuMapper;
import com.example.admin.data.vo.MenuVO;
import com.ttdys108.commons.exception.ErrorCode;
import com.ttdys108.commons.exception.ServiceException;
import com.ttdys108.commons.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class MenuService extends AbstractService<MenuMapper, Menu> {

    /**
     * 返回顶层menu树
     * @return menu
     */
    public Response<List<MenuVO>> queryMenus() {
        List<MenuVO> result = new ArrayList<>();
        List<MenuVO> voList = new ArrayList<>();
        List<Menu> menus = mapper.selectAll();
        menus.forEach(menu -> {
            MenuVO vo = new MenuVO();
            BeanUtils.copyProperties(menu, vo);
            voList.add(vo);
        });
        //build tree
        voList.forEach(vo -> {
            if(vo.getParent() == null) {
                result.add(vo); //result只返回顶层menulist
            } else {
                for(MenuVO v : voList) {
                    if(v.getId().equals(vo.getParent())) {
                        if(v.getChildren() == null)
                            v.setChildren(new ArrayList<>());
                        v.getChildren().add(vo);
                        break;
                    }
                }
            }

        });
        return Response.success(result);
    }

    public Response<Void> addMenu(Menu menu) throws ServiceException {
        checkAdd(menu);
        mapper.insertSelective(menu);
        return Response.success(null);
    }

    private void checkAdd(Menu menu) throws ServiceException {
        if(StringUtils.isAnyEmpty(menu.getCode(), menu.getPath(), menu.getDesc()) || menu.getStatus() == null)
            throw new ServiceException(ErrorCode.ILLEGAL_ARGUMENT);
        checkParent(menu.getParent());
        checkParams(menu, CheckType.code, ActionType.add);
        checkParams(menu, CheckType.path, ActionType.add);
    }

    public Response<Void> updateMenu(Menu menu) throws ServiceException {
        checkUpdate(menu);
        menu.setUpdateTime(new Date());
        mapper.updateByPrimaryKeySelective(menu);
        return Response.success(null);
    }

    public void checkUpdate(Menu menu) throws ServiceException {
        if(menu.getId() == null || StringUtils.isAnyEmpty(menu.getCode(), menu.getPath(), menu.getDesc()) || menu.getStatus() == null)
            throw new ServiceException(ErrorCode.ILLEGAL_ARGUMENT);
        checkParent(menu.getParent());
        checkParams(menu, CheckType.code, ActionType.update);
        checkParams(menu, CheckType.path, ActionType.update);
    }

    private void checkParent(Long pid) throws ServiceException {
        if(pid == null)
            return;
        Menu m = new Menu();
        m.setId(pid);
        Menu exists = mapper.selectOne(m);
        if(exists == null)
            throw new ServiceException(ErrorCode.ILLEGAL_ARGUMENT);
    }

    //校验code和path的唯一性
    private void checkParams(Menu menu, CheckType checkType, ActionType actionType) throws ServiceException {
        Menu m = new Menu();
        if(checkType == CheckType.code) {
            m.setCode(menu.getCode());
        } else {
            m.setPath(menu.getPath());
        }
        List<Menu> exists = mapper.select(m);
        if(actionType == ActionType.add && !exists.isEmpty())
            throw new ServiceException(ErrorCode.ILLEGAL_ARGUMENT);
        if(actionType == ActionType.update && !exists.isEmpty()) {
            Menu exist = exists.get(0);
            if(!exist.getId().equals(menu.getId()))
                throw new ServiceException(ErrorCode.ILLEGAL_ARGUMENT);
        }
    }


    private static enum CheckType {
        code, path;
    }
}
