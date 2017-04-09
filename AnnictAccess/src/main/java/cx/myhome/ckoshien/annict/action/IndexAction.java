/*
 * Copyright 2004-2008 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package cx.myhome.ckoshien.annict.action;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.seasar.framework.beans.util.BeanUtil;
import org.seasar.framework.util.ResourceUtil;
import org.seasar.struts.annotation.Execute;

import cx.myhome.ckoshien.annict.dto.LoginDto;
import cx.myhome.ckoshien.annict.rest.RestClient;
import cx.myhome.ckoshien.annict.rest.dto.AnnictAuthorizeDto;
import cx.myhome.ckoshien.annict.rest.dto.ProgramsDto;
import cx.myhome.ckoshien.annict.rest.dto.ResultDto;
import cx.myhome.ckoshien.annict.rest.dto.UserDto;


public class IndexAction {
	private static Logger logger = Logger.getLogger("rootLogger");
	public String code;
	public String result;
	public ResultDto resultDto;
	@Resource
	private LoginDto loginDto;
	private AnnictAuthorizeDto entity;
	private HashMap<String, String> header;
	private RestClient client;
	private ProgramsDto program;
	private List<ProgramsDto> programs;
	@Resource
	public HttpServletRequest request;
	@Resource
	public HttpServletResponse response;
	public String username;

    @Execute(validator = false)
	public String index() {
    	String host=request.getHeader("host");
    	if(code==null){
    		if(host.indexOf("localhost")!=-1){
    			return "https://annict.com/oauth/authorize?client_id=7867a6f7dff79dcc31ac4700e9ff1a95b2fce1092994cb68d7f38dcf92594066&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2FAnnictAccess&response_type=code&scope=read+write&redirect=true";
    		}else if(host.indexOf("192.168.11")!=-1){
    			return "https://annict.com/oauth/authorize?client_id=7867a6f7dff79dcc31ac4700e9ff1a95b2fce1092994cb68d7f38dcf92594066&redirect_uri=http%3A%2F%2F192.168.11.2%3A8080%2FAnnictAccess&response_type=code&scope=read+write&redirect=true";
    		}else{
    			return "https://annict.com/oauth/authorize?client_id=7867a6f7dff79dcc31ac4700e9ff1a95b2fce1092994cb68d7f38dcf92594066&redirect_uri=http%3A%2F%2Fjcbl.dip.jp%2FAnnictAccess&response_type=code&scope=read+write&redirect=true";
    		}
    	}
    	RestClient client = new RestClient();
		String uri = "https://api.annict.com/oauth/token";
		AnnictAuthorizeDto entity= new AnnictAuthorizeDto();
		HashMap<String, String> header= new HashMap<String, String>();
		entity.setClient_id(ResourceUtil.getProperties("config.properties").getProperty("client_id"));
		if(host.indexOf("localhost")!=-1){
			entity.setRedirect_uri("http://localhost:8080/AnnictAccess");
		}else if(host.indexOf("192.168.11")!=-1){
			entity.setRedirect_uri("http://192.168.11.2:8080/AnnictAccess");
		}else{
			entity.setRedirect_uri("http://jcbl.dip.jp/AnnictAccess");
		}
		entity.setResponse_type("code");
		entity.setScope("read");
		entity.setClient_secret(ResourceUtil.getProperties("config.properties").getProperty("secret_key"));
		entity.setGrant_type("authorization_code");
		entity.setCode(code);
		AnnictAuthorizeDto json=client.sendRequest(uri, "POST", entity, AnnictAuthorizeDto.class,header);
		loginDto.setAccess_token(json.getAccess_token());
		return "/index2&redirect=true";
	}

    @Execute(validator = false)
    public String index2(){
    	try {
    		InetAddress ia=InetAddress.getByName(request.getRemoteAddr());
//    		System.out.println(ia.getHostName());
//    		if(!ia.getHostName().substring(ia.getHostName().length()-3).equals(".jp")
//    				&& !request.getRemoteAddr().equals("0:0:0:0:0:0:0:1")
//    				&& !request.getRemoteAddr().startsWith("192.168")){
//    			//logger.info("ホスト名で遮断:"+ia.getHostName()+":"+request.getRemotePort());
//    			//response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//    			try {
//					response.sendError(404, "許可されていないドメインです");
//				} catch (IOException e) {
//					logger.error(e);
//				}
//        		return null;
//    		}
			logger.info(ia.getHostName()+":"+request.getRemotePort());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
    	if(loginDto.getAccess_token()==null){
    		return "401.jsp";
    	}
    	String uri="https://api.annict.com/v1/me/programs?sort_started_at=asc&per_page=40&filter_unwatched=true&accessToken="+loginDto.getAccess_token();
    	header= new HashMap<String, String>();
    	client = new RestClient();
    	header.put("Authorization", "Bearer "+loginDto.getAccess_token());
		entity=null;
		resultDto= new ResultDto();
		resultDto=client.sendRequest(uri, "GET", entity, ResultDto.class,header);
		Date now=new Date();
		programs=new ArrayList<ProgramsDto>();
		for(int i=0;i<resultDto.getPrograms().size();i++){
			ProgramsDto dto=new ProgramsDto();
			program=resultDto.getPrograms().get(i);
			Date started_at = program.getStarted_at();
			long dayDiff = ( now.getTime() - started_at.getTime()  ) / (1000 * 60 * 60 * 24 );
			//System.out.println(dayDiff);
			BeanUtil.copyProperties(program, dto);
			dto.setDayDiff(dayDiff);
			programs.add(dto);
		}
		resultDto.setPrograms(programs);
		System.out.println(loginDto.getAccess_token());
		uri = "https://api.annict.com/v1/me?access_token="+loginDto.getAccess_token();
		entity=null;
		UserDto json2=client.sendRequest(uri, "GET", entity, UserDto.class,header);
		username=json2.getName()+"("+json2.getUsername()+")";
		logger.info(username);
		return "index.jsp";
    }

    @Execute(validator = false)
	public String systemErr() {

		return "systemErr.jsp";
	}
}
