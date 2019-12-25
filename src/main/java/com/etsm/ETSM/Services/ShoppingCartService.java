/*
 * Copyright (c) 2019. Smalkov Nikita.
 */

package com.etsm.ETSM.Services;

import com.etsm.ETSM.Models.*;
import com.etsm.ETSM.Repositories.LoyaltyRepository;
import com.etsm.ETSM.Repositories.SalesRepository;
import com.etsm.ETSM.Repositories.Sales_has_productRepository;
import com.etsm.ETSM.Repositories.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.*;

public interface ShoppingCartService {
    public boolean addItemToCart(String code, HttpSession session);
    public boolean deleteItemFromCart(String code, HttpSession session);
    public boolean changeQuantity(String code, String type,HttpSession session);
    public void clearCart(HttpSession session);
    public boolean getTotalOrderPrice(HttpSession session);
    public boolean performOrder(HttpSession session, UserInfo userInfo);
    public boolean addFundsOnLoyalty(int amount, UserInfo userInfo);
}

@Service
class ShoppingCartServiceImpl implements ShoppingCartService {

    private UserInfoRepository userInfoRepository;
    private Sales_has_productRepository sales_has_productRepository;
    private SalesRepository salesRepository;
    private ProductService productService;
    private UserService userService;
    private LoyaltyRepository loyaltyRepository;

    @Override
    public boolean addItemToCart(String code, HttpSession session) {
        Long id = Long.parseLong(code);
        Product product = productService.findProductById(id).orElse(null);
        if (product == null)
            return false;
        if (session.getAttribute("cart") == null) {
            List<CartItem> cart = new ArrayList<>();
            cart.add(new CartItem(product,1));
            session.setAttribute("cart",cart);
        } else {
            List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
            int index = this.exists(id, cart);
            if (index == -1) {
                cart.add(new CartItem(product,1));
            } else {
                changeQuantity(code,"plus",session);
            }
            session.setAttribute("cart",cart);
        }
        return true;
    }

    @Override
    public boolean deleteItemFromCart(String code, HttpSession session) {
        Long id = Long.parseLong(code);
        Product product = productService.findProductById(id).orElse(null);
        if (product == null)
            return false;
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        int index = this.exists(id,cart);
        if (index == -1)
            return false;
        cart.remove(index);
        session.setAttribute("cart",cart);
        return true;
    }

    @Override
    public boolean changeQuantity(String code, String type,HttpSession session) {
        Long id = Long.parseLong(code);
        Product product = productService.findProductById(id).orElse(null);
        if (product == null)
            return false;
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        int index = this.exists(id,cart);
        if (index == -1)
            return false;
        int quantity = cart.get(index).getQuantity();
        if (type.equals("plus")) {
            quantity++;
            cart.get(index).setQuantity(quantity);
            cart.get(index).setTotalPrice();
        } else if (type.equals("minus")) {
            quantity--;
            cart.get(index).setQuantity(quantity);
            cart.get(index).setTotalPrice();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean getTotalOrderPrice(HttpSession session) {
        if (session.getAttribute("cart") == null) {
            return false;
        } else {
            List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
            int totalOrderPrice = 0;
            for (CartItem cartItem : cart) {
                totalOrderPrice+=cartItem.getTotalPrice();
            }
            session.setAttribute("totalOrderPrice",totalOrderPrice);
            return true;
        }
    }

    @Override
    public void clearCart(HttpSession session) {
        if (session.getAttribute("cart") != null) {
            session.removeAttribute("cart");
        }
    }

    private int exists(Long id, List<CartItem> cart) {
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getProduct().getId() == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean performOrder(HttpSession session, UserInfo userInfo) {
        Sales newSale = new Sales();
        int sum = (int)session.getAttribute("totalOrderPrice");
        List<Sales_has_product> salesHasProducts = new ArrayList<>();
        if (userInfo.getSales() == null) {
            userInfo.setSales(new ArrayList<>());
        }
        addFundsOnLoyalty(sum,userInfo);
        newSale.setSum(sum);
        newSale.setUserInfoId(userInfo);
        salesRepository.saveAndFlush(newSale);
        newSale.setSalesHasProducts(salesHasProducts);
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        for (CartItem cartItem : cart) {
            Sales_has_product hasProduct = new Sales_has_product();
            if (cartItem.getProduct().getSalesHasProducts() == null) {
                cartItem.getProduct().setSalesHasProducts(new ArrayList<>());
            }
            hasProduct.setCount(cartItem.getQuantity());
            hasProduct.setSumm(cartItem.getTotalPrice());
            hasProduct.setDiscount(0);
            hasProduct.setSales_id(newSale);
            hasProduct.setProduct_id(cartItem.getProduct());
            sales_has_productRepository.saveAndFlush(hasProduct);
        }
        salesRepository.saveAndFlush(newSale);
        clearCart(session);
        return true;
    }

    @Override
    public boolean addFundsOnLoyalty(int amount, UserInfo userInfo) {
        Loyalty loyalty = loyaltyRepository.findById(userInfo.getLoyaltyCode_id().getId()).get();
        int addedFunds = amount / 10;
        int newFunds = loyalty.getBalance() + addedFunds;
        loyalty.setBalance(addedFunds);
        loyaltyRepository.saveAndFlush(loyalty);
        return true;
    }

    @Autowired
    public void setUserInfoRepository(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }
    @Autowired
    public void setSales_has_productRepository(Sales_has_productRepository sales_has_productRepository) {
        this.sales_has_productRepository = sales_has_productRepository;
    }
    @Autowired
    public void setSalesRepository(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }
    @Autowired
    public void setProductService(ProductService productService) {
        this.productService = productService;
    }
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    @Autowired
    public void setLoyaltyRepository(LoyaltyRepository loyaltyRepository) {
        this.loyaltyRepository = loyaltyRepository;
    }
}