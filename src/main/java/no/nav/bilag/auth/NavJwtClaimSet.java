package no.nav.bilag.auth;

import com.nimbusds.jwt.JWTClaimsSet;

public record NavJwtClaimSet(JWTClaimsSet jwtClaimsSet) {

	public String getName() {
		return (String) jwtClaimsSet.getClaim("name");
	}

	public String getNavIdent() {
		return (String) jwtClaimsSet.getClaim("NAVident");
	}
}
