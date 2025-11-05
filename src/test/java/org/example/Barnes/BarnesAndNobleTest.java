package org.example.Barnes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;



class BarnesAndNobleTest {

    private BookDatabase bookDatabase;
    private BuyBookProcess process;
    private BarnesAndNoble store;


    @BeforeEach
    void setUp(){
        bookDatabase= mock(BookDatabase.class);
        process= mock(BuyBookProcess.class);
        store= new BarnesAndNoble(bookDatabase, process);
    }

    /**
     * specification based testing
     * Inputs ->  output
     * 1. ISBN (String):
     *  a. Valid ISBN that exist in database -> process book
     *  b. Null ISBN  ->  throws NullPointerException
     *  c. Empty string ""  ->  throws NullPointerException
     *
     * 2. quantity (Integer):
     *  a. quantity = 0  -> totalPrice += 0
     *  b. quantity = 1  -> totalPrice += (1* book.price)
     *  c. quantity > available stock  -> only charge for available book, mark the rest as unavailable
     *  d. quantity <= available stock  -> charge for all the quantity, unavailable would still be empty
     *
     * 3.order (Map<String, Integer>)
     *  a. Null map -> return null
     *  b. Empty map {} -> return empty summary (totalPrice=0, unavailable={})
     *  c. Single entry map {"ISBN1": 4}  -> process just 1 book
     *  d. Multiple entries map {"ISBN1": 4, "ISBN2": 3} -> process 2 books
     *
     *
     * Input Combinations -> output
     * purchaseSummary:
     * a. Null order -> return null
     * b. Empty order {} -> returns totalPrice = 0, unavailable = {}
     * c. Single order, quantity <= available stock -> returns totalPrice > 0, unavailable = {}
     * d. Single order,  quantity > available stock -> returns totalPrice (available *price ), unavailable = {unavailable book}
     * e. Multiple orders, quantity <= available stock ->  returns totalPrice= sum of all, unavailable = {}
     * f. Multiple orders, mixed stock (some <=, some < )   -> return totalPrice= sum of (available* price) , unavailable = { unavailable book/ books}
     *
     */

   @Test
    @DisplayName ("specification based testing")
    void getPriceForCart_withNullOrder(){
        PurchaseSummary result= store.getPriceForCart(null);
        assertNull(result);
    }

    @Test
    @DisplayName ("specification based testing")
    void getPriceCart_withEmptyOrder(){
        PurchaseSummary result= store.getPriceForCart(new HashMap<>());
        //verify
        assertEquals(0, result.getTotalPrice());
        assertTrue(result.getUnavailable().isEmpty());
    }

    @Test
    @DisplayName ("specification based testing")
    void getPriceCart_withSingleOrder_fullStock(){
        //creating the book
        Book book = new Book("ISBN1", 25, 20);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);

        //creating an order
        Map<String, Integer> order= new HashMap<>();
        order.put("ISBN1", 6);

        PurchaseSummary result= store.getPriceForCart(order);
        int expectedPrice= 6 * 25;
        assertEquals( expectedPrice, result.getTotalPrice());
        assertTrue(result.getUnavailable().isEmpty());


    }
    @Test
    @DisplayName ("specification based testing")
    void getPriceCart_withSingleOrder_partialStock(){
        Book book = new Book("ISBN1", 25, 20);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order= new HashMap<>();
        order.put("ISBN1", 21);

        PurchaseSummary result= store.getPriceForCart(order);
        int expectedPrice= 20 * 25;
        assertEquals( expectedPrice, result.getTotalPrice());
        assertEquals(1, result.getUnavailable().get(book));

    }

    @Test
    @DisplayName("Single order with quantity = 0")
    void getPriceCart_withSingleOrder_zeroQuantity(){
        Book book = new Book("ISBN1", 25, 20);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 0);  // ← Quantity = 0

        PurchaseSummary result = store.getPriceForCart(order);

        assertEquals(0, result.getTotalPrice());  // 0 × 25 = 0
        assertTrue(result.getUnavailable().isEmpty());
    }

    @Test
    @DisplayName("Single order with quantity = 1")
    void getPriceCart_withSingleOrder_quantityOne(){
        Book book = new Book("ISBN1", 25, 20);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 1);  // ← Quantity = 1

        PurchaseSummary result = store.getPriceForCart(order);

        assertEquals(25, result.getTotalPrice());  // 1 × 25 = 25
        assertTrue(result.getUnavailable().isEmpty());
    }
    @Test
    @DisplayName ("specification based testing")
    void getPriceCart_withMultipleOrder_fullStock(){
        Book book1 = new Book("ISBN1", 25, 20);
        Book book2= new Book("ISBN2", 40, 15);
        Book book3= new Book("ISBN3", 35, 10);

        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(bookDatabase.findByISBN("ISBN2")).thenReturn(book2);
        when(bookDatabase.findByISBN("ISBN3")).thenReturn(book3);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 10);
        order.put("ISBN2", 15);
        order.put("ISBN3", 10);

        PurchaseSummary result= store.getPriceForCart(order);
        assertEquals((10*25) + (15 *40) + (10*35) , result.getTotalPrice());
        assertTrue(result.getUnavailable().isEmpty());

    }
    @Test
    @DisplayName ("specification based testing")
    void getPriceCart_withMultipleOrder_mixedStock(){
        Book book1 = new Book("ISBN1", 25, 20);
        Book book2= new Book("ISBN2", 40, 15);
        Book book3= new Book("ISBN3", 35, 8);

        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(bookDatabase.findByISBN("ISBN2")).thenReturn(book2);
        when(bookDatabase.findByISBN("ISBN3")).thenReturn(book3);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 10);
        order.put("ISBN2", 15);
        order.put("ISBN3", 15);

        PurchaseSummary result= store.getPriceForCart(order);
        assertEquals((10*25) + (15 *40) + (8*35) , result.getTotalPrice());
        assertEquals(7, result.getUnavailable().get(book3));

    }






    @Test
    @DisplayName("structural-based")
    void getPriceForCart_nullOrder(){
        PurchaseSummary result = store. getPriceForCart(null);
        assertNull(result);
    }

    @Test
    @DisplayName("structural-based: getPriceForCart method")
    void getPriceForCart_emptyOrder(){
        PurchaseSummary result = store.getPriceForCart(new HashMap<>());
        assertEquals(0, result.getTotalPrice());
        assertTrue(result.getUnavailable().isEmpty());
    }

    @Test
    @DisplayName("structural-based: getPriceForCart method")
    void retrieveBook_addUnavailableCalled(){
        Book book = new Book("ISBN1", 50, 5);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);
        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 10);
        PurchaseSummary result = store.getPriceForCart(order);
        assertEquals(5 * 50, result.getTotalPrice());
        assertEquals(5, result.getUnavailable().get(book));
        verify(process).buyBook(book, 5);
    }
    @Test
    @DisplayName("structural-based: retrieveBook method")
    void retrieveBook_quantityEqualsStock(){
        Book book = new Book("ISBN1", 30, 10);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 10);
        PurchaseSummary result = store.getPriceForCart(order);

        assertEquals(10 * 30, result.getTotalPrice());
        assertTrue(result.getUnavailable().isEmpty());
        verify(process).buyBook(book, 10);
    }

    @Test
    @DisplayName("structural-based: retrieveBook method")
    void retrieveBook_multipleLoopIterations(){
        Book book1 = new Book("ISBN1", 20, 10);
        Book book2 = new Book("ISBN2", 30, 5);
        when(bookDatabase.findByISBN("ISBN1")).thenReturn(book1);
        when(bookDatabase.findByISBN("ISBN2")).thenReturn(book2);

        Map<String, Integer> order = new HashMap<>();
        order.put("ISBN1", 5);
        order.put("ISBN2", 3);
        PurchaseSummary result = store.getPriceForCart(order);

        // Verifying that  retrieveBook() called twice
        verify(bookDatabase).findByISBN("ISBN1");
        verify(bookDatabase).findByISBN("ISBN2");
        verify(process).buyBook(book1, 5);
        verify(process).buyBook(book2, 3);

        assertEquals((5 * 20) + (3 * 30), result.getTotalPrice());

    }
    @Test
    @DisplayName("structural-based: getPurchaseSummary class")
    void purchaseSummaryAddUnavailable(){
        PurchaseSummary summary = new PurchaseSummary();
        Book book = new Book("ISBN1", 20, 10);

        summary.addUnavailable(book, 3);
        assertEquals(3, summary.getUnavailable().get(book));
    }

    @Test
    @DisplayName("structural-based: getPurchaseSummary class")
    void purchaseSummaryAddToTotalPrice(){
        PurchaseSummary summary = new PurchaseSummary();
        summary.addToTotalPrice(100);
        assertEquals(100, summary.getTotalPrice());
        summary.addToTotalPrice(50);
        assertEquals(150, summary.getTotalPrice());
    }

    @Test
    @DisplayName("structural-based: getPurchaseSummary class")
    void purchaseSummaryGetTotalPrice(){
        PurchaseSummary summary = new PurchaseSummary();
        assertEquals(0, summary.getTotalPrice());
        summary.addToTotalPrice(75);
        assertEquals(75, summary.getTotalPrice());
    }

    @Test
    @DisplayName("structural-based: PurchaseSummary class")
    void coverage_purchaseSummaryGetUnavailable(){
        PurchaseSummary summary = new PurchaseSummary();
        assertTrue(summary.getUnavailable().isEmpty());

        Book book = new Book("ISBN1", 20, 10);
        summary.addUnavailable(book, 2); //adding book to the unavailable
        assertEquals(1, summary.getUnavailable().size());
    }

    @Test
    @DisplayName("structural-based: Book constructor and getters")
    void coverage_bookConstructorAndGetters(){
        Book book = new Book("ISBN123", 50, 10);

        assertEquals(50, book.getPrice());
        assertEquals(10, book.getQuantity());
    }
    @Test
    @DisplayName("structural-based: Book constructor and getters")
    void bookEquals(){
        Book book1 = new Book("ISBN1", 20, 10);
        Book book2 = new Book("ISBN1", 30, 5);   // Same ISBN, different price/qty
        Book book3 = new Book("ISBN2", 20, 10);  // Different ISBN

        assertTrue(book1.equals(book2));   // Same ISBN
        assertFalse(book1.equals(book3));  // Different ISBN
        assertFalse(book1.equals(null));   // Null comparison
    }
    @Test
    @DisplayName("structural-based: Book constructor and getters")
    void bookEquals_sameReference(){
        Book book = new Book("ISBN1", 20, 10);
        // Comparing object to itself
        assertTrue(book.equals(book));
    }
    @Test
    @DisplayName("structural-based: Book constructor and getters")
    void bookEquals_nullObject(){
        Book book = new Book("ISBN1", 20, 10);
        // Comparing to null
        assertFalse(book.equals(null));
    }
    @Test
    @DisplayName("structural-based: Book constructor and getters")
    void bookEquals_differentClass(){
        Book book = new Book("ISBN1", 20, 10);
        String notABook = "ISBN1";
        // Comparing Book to String (different classes)
        assertFalse(book.equals(notABook));
    }




    @Test
    @DisplayName("structural-based: Book constructor and getters")

    void bookHashCode(){
        Book book1 = new Book("ISBN1", 20, 10);
        Book book2 = new Book("ISBN1", 20, 10);

        assertEquals(book1.hashCode(), book2.hashCode());
    }

}











