package com.side.shop.security.jwt;

import com.side.shop.member.domain.MemberRole;
import com.side.shop.security.auth.CustomUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. Request Header에서 JWT 토큰 추출
            String token = getJwtFromRequest(request);

            // 2. 토큰 유효성 검증
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                // 3. 토큰에서 정보 추출
                Long memberId = jwtTokenProvider.getMemberIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);

                // 4. UserDetails 생성 (DB 조회 없이 토큰 정보로만 생성)
                UserDetails userDetails = new CustomUserDetails(memberId, email, MemberRole.valueOf(role));

                // 5. Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. SecurityContext에 Authentication 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute("exception", "EXPIRED_TOKEN");
        } catch (UnsupportedJwtException ex) {
            request.setAttribute("exception", "UNSUPPORTED_TOKEN");
        } catch (SecurityException | MalformedJwtException ex) {
            request.setAttribute("exception", "INVALID_TOKEN");
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            request.setAttribute("exception", "UNKNOWN_ERROR");
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 JWT 토큰 추출
     * Authorization: Bearer <token>
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 토큰 부분만 추출
        }
        return null;
    }
}
