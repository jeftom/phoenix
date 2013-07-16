package com.dianping.phoenix.service.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dianping.phoenix.agent.resource.entity.Domain;
import com.dianping.phoenix.agent.resource.entity.Product;
import com.dianping.phoenix.agent.resource.entity.Resource;

public interface ResourceManager {

	public Resource getResource();

	public Domain updateDomainManually(String name);

	public Domain getDomain(String name);

	public List<Product> getProducts();

	public Map<String, Set<String>> getLibSet();
}