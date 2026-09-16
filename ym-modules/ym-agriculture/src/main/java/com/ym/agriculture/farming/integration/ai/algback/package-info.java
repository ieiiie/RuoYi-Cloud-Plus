/**
 * 算法中台（alg_back）对接，按功能分包：
 * <ul>
 *   <li>{@link com.ym.agriculture.farming.integration.ai.algback.api} — Retrofit API</li>
 *   <li>{@link com.ym.agriculture.farming.integration.ai.algback.client} — OkHttp/Retrofit、同步调用、异常</li>
 *   <li>{@link com.ym.agriculture.farming.integration.ai.algback.auth} — 登录、token、拦截器、定时续期、JWT</li>
 *   <li>{@link com.ym.agriculture.farming.integration.ai.algback.autoconfigure} — Spring Boot 配置</li>
 *   <li>{@code com.ym.agriculture.farming.integration.ai.algback.dto} — 请求与通用响应 DTO</li>
 * </ul>
 *
 * @author ym-cloud
 */
package com.ym.agriculture.farming.integration.ai.algback;
