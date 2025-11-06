package org.example.Amazon;

import org.example.Amazon.Cost.PriceRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmazonUnitTest {
    private Amazon amazon;
    private ShoppingCart mockedCart;
    private List<PriceRule> mockedRules;
    private PriceRule mockedRegularCost;
    private PriceRule mockedDeliveryPrice;
    private PriceRule mockedExtraCostForElectronics;
    private Item mockedItem;

    @BeforeEach
    void setUp(){
        mockedCart = mock(ShoppingCart.class);
        mockedRegularCost= mock(PriceRule.class);
        mockedDeliveryPrice= mock(PriceRule.class);
        mockedExtraCostForElectronics= mock(PriceRule.class);
        mockedRules= new ArrayList<>(Arrays.asList(mockedRegularCost, mockedDeliveryPrice, mockedExtraCostForElectronics));
        mockedItem= mock(Item.class);

        amazon= new Amazon(mockedCart, mockedRules); //initializing amazon with the mocked shopping carts and mocked rules
    }

    @Nested
    @DisplayName("specification-based: calculate()")
    class CalculateSpecificationTests {

        @Test
        @DisplayName("testing to return the sum of all price rules")
        void sumAllPriceRules(){
            // arrange
            List<Item> items = Arrays.asList(mock(Item.class));
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(200.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(20.0);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(30.0);

            // act (calling the method)
            double result = amazon.calculate();

            //verify
            assertEquals(200+20 +30, result);
        }

        @Test
        @DisplayName("test for handling single price rule")
        void calculateSinglePriceRule(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            amazon= new Amazon (mockedCart, Arrays.asList(mockedRegularCost));
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(75.0);
            double result= amazon.calculate();
            assertEquals(75.0, result);
        }

        @Test
        @DisplayName("test to return 0 when there is no rule for price")
        void calculatePriceWithNoRules(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            amazon= new Amazon (mockedCart, new ArrayList<>());
            double result= amazon.calculate();
            assertEquals(0.0, result);
        }
        @Test
        @DisplayName("tests for handling negative prices (discounts)")
        void calculateNegativePrices(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(200.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(-20.0);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(30.0);

            double result = amazon.calculate();
            assertEquals(200-20+30, result);
        }

        @Test
        @DisplayName("tests for handling decimal prices")
        void calculateDecimalPrices(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(80.99);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(9.99);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(30.0);

            double result = amazon.calculate();
            assertEquals(80.99 + 9.99+ 30.0, result);
        }

        @Test
        @DisplayName("test for deletgating retrieving items to cart when calculatig the price")
        void shouldFetchItemsFromCartDuringCalculation(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(59.99);
            amazon = new Amazon(mockedCart, Arrays.asList(mockedRegularCost));
            amazon.calculate();
            verify(mockedCart, times(1)).getItems();
        }

        @Test
        @DisplayName("test to return 0 when all rules return 0")
        void calculateWhenAllRulesReturnZero(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(0.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(0.0);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(0.0);
            double result = amazon.calculate();
            assertEquals(0, result);
        }
    }

    @Nested
    @DisplayName("specification-based: addToCard()")
    class addToCardSpecificationTests {

        @Test
        @DisplayName("test for adding single item to the cart")
        void addSingleItemToCart(){
            Item item = mock(Item.class);
            amazon.addToCart(item);
            verify(mockedCart, times(1)).add(item);
        }
        @Test
        @DisplayName("test for adding mutiple items to the cart sequentially")
        void addMultipleItemsToCart(){
            Item item1= mock(Item.class);
            Item item2= mock(Item.class);
            Item item3= mock(Item.class);
            Item item4= mock(Item.class);
            amazon.addToCart(item1);
            amazon.addToCart(item2);
            amazon.addToCart(item3);
            amazon.addToCart(item4);

            //verifying that all items were added
            verify(mockedCart, times(4)).add(any(Item.class));
        }
        @Test
        @DisplayName("test for accepting null item")
        void addNullItem(){
            amazon.addToCart(null);
            verify(mockedCart, times(1)).add(null);
        }

    }


    @Nested
    @DisplayName("structured-based: calculate()")
    class CalculateStructureTests{

        @Test
        @DisplayName("test so that loop executes 0 time when rules list is empty")

        void loopZeroTimes(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            amazon = new Amazon(mockedCart, new ArrayList<>());
            double result = amazon.calculate();
            assertEquals(0, result);
            verify( mockedRegularCost, never()).priceToAggregate(any());
        }

        @Test
        @DisplayName("test so that loop executes 1 time when 1 rule exist")
        void loopOneTimes(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(45.0);
            amazon = new Amazon(mockedCart, Arrays.asList(mockedRegularCost));
            double result = amazon.calculate();
            assertEquals(45.0, result);
            verify( mockedRegularCost, times(1)).priceToAggregate(items);
        }


        @Test
        @DisplayName("test so that loop executes multiple time when multiple rules exist")
        void loopTwoTimes(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(45.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(10.0);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(20.0);

            double result = amazon.calculate();
            assertEquals(45.0 + 10.0 + 20.0 , result);
            verify( mockedRegularCost, times(1)).priceToAggregate(items);
            verify(mockedDeliveryPrice, times(1)).priceToAggregate(items);
            verify(mockedExtraCostForElectronics, times(1)).priceToAggregate(items);
        }
        @Test
        @DisplayName("test finalPrice ")
        void calculateFinalPrice(){
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(45.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(10.0);
            when(mockedExtraCostForElectronics.priceToAggregate(items)).thenReturn(20.0);
            double result = amazon.calculate();
            assertEquals(75.0, result);
        }


        @Test
        @DisplayName("test so that rules are invoked in correct sequence order")
        void testRulesExecuteInOrder() {
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(80.0);
            when(mockedDeliveryPrice.priceToAggregate(items)).thenReturn(9.0);
            amazon.calculate();
            InOrder inOrder = inOrder(mockedRegularCost, mockedDeliveryPrice);
            inOrder.verify(mockedRegularCost).priceToAggregate(items);
            inOrder.verify(mockedDeliveryPrice).priceToAggregate(items);
        }


        @Test
        @DisplayName("test so that getItems() is called to fetch items from cart")
        void testGetItemsIsCalled() {
            List<Item> items = new ArrayList<>();
            when(mockedCart.getItems()).thenReturn(items);
            when(mockedRegularCost.priceToAggregate(items)).thenReturn(10.0);
            amazon = new Amazon(mockedCart, Arrays.asList(mockedRegularCost));
            amazon.calculate();
            verify(mockedCart, times(1)).getItems();
        }

    }

    @Nested
    @DisplayName("structure-based: addToCart()")
    class AddToCartStructureTests {
        @Test
        @DisplayName("test for single call to the cardt.add()")
        void singleCallToAdd(){
            Item item = mock(Item.class);
            amazon.addToCart(item);
            verify(mockedCart, times(1)).add(item);
        }
        @Test
        @DisplayName("test for one call to add() per invocation")
        void oneCallPerInvocation(){
            Item item1 = mock(Item.class);
            Item item2 = mock(Item.class);
            amazon.addToCart(item1);
            amazon.addToCart(item2);
            verify(mockedCart, times(2)).add(any(Item.class));
        }


        @Test
        @DisplayName("test to pass item unchanged to cart")
        void passItemUnchanged() {
            Item item = mock(Item.class);
            amazon.addToCart(item);
            verify(mockedCart).add(argThat(arg -> arg == item));
        }
    }
}