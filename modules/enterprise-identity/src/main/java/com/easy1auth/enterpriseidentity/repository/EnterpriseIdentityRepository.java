package com.easy1auth.enterpriseidentity.repository;

import com.easy1auth.enterpriseidentity.model.EnterpriseIdentitySourceEntity;
import org.babyfish.jimmer.spring.repository.JRepository;
import java.util.UUID;

/** 企业身份源及同步数据访问仓储。 */
public interface EnterpriseIdentityRepository extends JRepository<EnterpriseIdentitySourceEntity, UUID> {
}
