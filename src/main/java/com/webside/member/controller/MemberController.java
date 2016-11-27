package com.webside.member.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.webside.base.basecontroller.BaseController;
import com.webside.dtgrid.model.Pager;
import com.webside.dtgrid.util.ExportUtils;
import com.webside.exception.AjaxException;
import com.webside.member.mapper.MemberMapper;
import com.webside.member.model.MemberEntity;
import com.webside.member.service.MemberService;
import com.webside.user.model.UserEntity;
import com.webside.util.Common;
import com.webside.util.PageUtil;
import com.webside.village.model.Village;

@Controller
@Scope("prototype")
@RequestMapping("/member/")
public class MemberController extends BaseController {
	@Autowired
	MemberService memberservice;

	@RequestMapping("listUI.html")
	public String listUI(Model model, HttpServletRequest request) {
		try {
			PageUtil page = new PageUtil();
			if (request.getParameterMap().containsKey("page")) {
				page.setPageNum(Integer.valueOf(request.getParameter("page")));
				page.setPageSize(Integer.valueOf(request.getParameter("rows")));
				page.setOrderByColumn(request.getParameter("sidx"));
				page.setOrderByType(request.getParameter("sord"));
			}
			model.addAttribute("page", page);
			return Common.BACKGROUND_PATH + "/member/list";
		} catch (Exception e) {
			throw new AjaxException(e);
		}
	}

	/**
	 * ajax分页动态加载模式
	 * 
	 * @param dtGridPager
	 *            Pager对象
	 * @throws Exception
	 */
	@RequestMapping(value = "/list.html", method = RequestMethod.POST)
	@ResponseBody
	public Object list(String gridPager, HttpServletResponse response) throws Exception {
		Map<String, Object> parameters = null;
		// 1、映射Pager对象
		Pager pager = JSON.parseObject(gridPager, Pager.class);
		// 2、设置查询参数
		// 获取登录用户
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		UserEntity sessionUser = (UserEntity) request.getSession().getAttribute("userSession");
		parameters = pager.getParameters();
		//判断是否为空，不为空则显示当前村居，为空则显示全部（管理员权限）
		
		if (sessionUser.getUserInfo().getVillageid()!=null) {
			parameters.put("villageid", sessionUser.getUserInfo().getVillageid());
		} else {
			parameters.put("villageid", null);
		}
		// 3、判断是否是导出操作
		if (pager.getIsExport()) {
			if (pager.getExportAllData()) {
				// 3.1、导出全部数据
				List<MemberEntity> list = memberservice.queryListByPage(parameters);
				ExportUtils.exportAll(response, pager, list);
				return null;
			} else {
				// 3.2、导出当前页数据
				ExportUtils.export(response, pager);
				return null;
			}
		} else {
			// 设置分页，page里面包含了分页信息
			Page<Object> page = PageHelper.startPage(pager.getNowPage(), pager.getPageSize(), "m_id DESC");
			List<MemberEntity> list = memberservice.queryListByPage(parameters);
			parameters.clear();
			parameters.put("isSuccess", Boolean.TRUE);
			parameters.put("nowPage", pager.getNowPage());
			parameters.put("pageSize", pager.getPageSize());
			parameters.put("pageCount", page.getPages());
			parameters.put("recordCount", page.getTotal());
			parameters.put("startRecord", page.getStartRow());
			// 列表展示数据
			parameters.put("exhibitDatas", list);
			return parameters;
		}
	}

	
	@RequestMapping("add.html")
	@ResponseBody
	public Object add(MemberEntity memberEntity) throws AjaxException {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		UserEntity sessionUser = (UserEntity) request.getSession().getAttribute("userSession");
	
		if (sessionUser.getUserInfo().getVillageid()!=null) {		
			memberEntity.setmVid(Integer.parseInt(sessionUser.getUserInfo().getVillageid()));
		} 
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			int result = this.memberservice.insert(memberEntity);
			
			if (result == 1) {
				map.put("success", Boolean.TRUE);
				map.put("data", null);
				map.put("message", "添加成功");
			} else {
				map.put("success", Boolean.FALSE);
				map.put("data", null);
				map.put("message", "添加失败");
			}
			return map;
		} catch (Exception e) {
		
			throw new AjaxException(e);
			
		}
	}
	
	@RequestMapping("addUI.html")
	public String addUI(Model model) {
		try {
			return Common.BACKGROUND_PATH + "/member/form";
		} catch (Exception e) {
			throw new AjaxException(e);
		}

	}
}
