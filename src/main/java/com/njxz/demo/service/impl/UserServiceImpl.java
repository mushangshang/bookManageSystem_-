package com.njxz.demo.service.impl;

import com.njxz.demo.domain.*;
import com.njxz.demo.domain.Vo.BookVo;
import com.njxz.demo.domain.Vo.BorrowingBooksVo;
import com.njxz.demo.mapper.BookMapper;
import com.njxz.demo.mapper.BorrowingBooksMapper;
import com.njxz.demo.mapper.DepartmentMapper;
import com.njxz.demo.mapper.UserMapper;
import com.njxz.demo.service.IUserService;
import com.njxz.demo.utils.page.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private BorrowingBooksMapper borrowingBooksMapper;
    @Resource
    private BookMapper bookMapper;
    @Override
    public List<User> findUserByUserName(String userName) {
        UserExample userExample=new UserExample();
        UserExample.Criteria criteria=userExample.createCriteria();
        criteria.andUserNameEqualTo(userName);
        List<User> users=userMapper.selectByExample(userExample);
        return users;
    }

    @Override
    public List<Department> findAllDepts() {

        return departmentMapper.selectByExample(new DepartmentExample());
    }

    @Override
    public User userLogin(String userName, String password) {
        List<User> users=findUserByUserName(userName);
        if(null==users){
            return null;
        }
        for(User user:users){
            if(user.getUserPwd().equals(password)){
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean updateUserByAdmin(User user) {
        int n=userMapper.updateByPrimaryKey(user);
        if(n>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean updateUser(User user, HttpServletRequest request) {
        //??????session?????????user??????
        User sessionUser= (User) request.getSession().getAttribute("user");
        user.setUserId(sessionUser.getUserId());
        int n=userMapper.updateByPrimaryKey(user);
        if(n>0){
            //?????????????????????session??????
            User newUser=userMapper.selectByPrimaryKey(user.getUserId());
            request.getSession().setAttribute("user",newUser);
            return true;
        }
        return false;
    }

    @Override
    public List<BorrowingBooksVo> findAllBorrowingBooks(HttpServletRequest request) {
        User user= (User) request.getSession().getAttribute("user");

        //?????????????????? userId
        BorrowingBooksExample borrowingBooksExample=new BorrowingBooksExample();
        BorrowingBooksExample.Criteria criteria=borrowingBooksExample.createCriteria();
        criteria.andUserIdEqualTo(user.getUserId());
        List<BorrowingBooks> borrowingBooksList=borrowingBooksMapper.selectByExample(borrowingBooksExample);
        if(null==borrowingBooksList){
            return null;
        }
        //??????????????????????????????(Do)???????????????????????????VO???
        List<BorrowingBooksVo> res=new LinkedList<BorrowingBooksVo>();
        for(BorrowingBooks borrowingBooks:borrowingBooksList){
            Book book=bookMapper.selectByPrimaryKey(borrowingBooks.getBookId());
            BorrowingBooksVo borrowingBooksVo=new BorrowingBooksVo();
            borrowingBooksVo.setBook(book);

            //????????????
            Date date1=borrowingBooks.getDate();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            String dateOfBorrowing=sdf.format(date1);

            //??????????????????
            Calendar calendar=Calendar.getInstance();
            calendar.setTime(date1);
            calendar.add(Calendar.MONTH,2);//???????????????
            Date date2=calendar.getTime();
            String dateOfReturn =sdf.format(date2);

            borrowingBooksVo.setDateOfBorrowing(dateOfBorrowing);
            borrowingBooksVo.setDateOfReturn(dateOfReturn);
            res.add(borrowingBooksVo);
        }
        return res;
    }

    @Override
    public boolean userReturnBook(int BorrowingId, HttpServletRequest request) {
        User user= (User) request.getSession().getAttribute("user");
        BorrowingBooksExample borrowingBooksExample=new BorrowingBooksExample();
        BorrowingBooksExample.Criteria criteria=borrowingBooksExample.createCriteria();
        criteria.andIdEqualTo(BorrowingId);
        Book book = new Book();

        List<BorrowingBooks> borrowingBooksList = borrowingBooksMapper.selectByExample(borrowingBooksExample);
        for(BorrowingBooks b:borrowingBooksList){
            book.setBookId(b.getBookId());
        }

        BookExample bookExample = new BookExample();
        BookExample.Criteria criteria1 = bookExample.createCriteria();
        criteria1.andBookIdEqualTo(book.getBookId());
        List<Book> books = bookMapper.selectByExample(bookExample);

        int bookNumber = 0;
        for (Book b : books) {
            bookNumber = b.getBookNumber();
        }
        if (bookNumber == 0) {
            return false;
        }else{
            bookNumber++;
            book.setBookNumber(bookNumber);
            bookMapper.updateNumber(book);
        }

        //??????????????????user_id??????userId,book_id??????bookId?????????
        int n=borrowingBooksMapper.deleteByExample(borrowingBooksExample);
        if(n>0) return true;
        return false;
    }

    @Override
    public boolean userBorrowingBook(int bookId, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");

        //????????????????????????,????????????????????????????????????????????????
        BookExample bookExample = new BookExample();
        BookExample.Criteria criteria = bookExample.createCriteria();
        criteria.andBookIdEqualTo(bookId);
        List<Book> books = bookMapper.selectByExample(bookExample);

        Book book = new Book();
        book.setBookId(bookId);
        int bookNumber = 0;
        for (Book b : books) {
            bookNumber = b.getBookNumber();
        }
        if (bookNumber == 0) {
            return false;
        }else{
            bookNumber--;
            book.setBookNumber(bookNumber);
            bookMapper.updateNumber(book);
        }

        BorrowingBooksExample borrowingBooksExample=new BorrowingBooksExample();
        BorrowingBooksExample.Criteria criteria1=borrowingBooksExample.createCriteria();
        criteria1.andBookIdEqualTo(bookId);
        List<BorrowingBooks> borrowingBooksList=borrowingBooksMapper.selectByExample(borrowingBooksExample);

        BorrowingBooks borrowingBooks = new BorrowingBooks();
        borrowingBooks.setBookId(bookId);

        borrowingBooks.setUserId(user.getUserId());
        borrowingBooks.setDate(new Date());
        //????????????????????????????????????
        int n = borrowingBooksMapper.insert(borrowingBooks);
        if (n > 0) {
            return true;
        }
        return false;
    }

    @Override
    public User findUserById(int id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public Page<User> findUserByPage(int pageNum) {
        List<User> users=userMapper.selectByPageNum((pageNum-1)*10,10);
        Page<User> page=new Page<>();
        page.setList(users);
        page.setPageNum(pageNum);
        page.setPageSize(10);

        int userCount=userMapper.selectUserCount();
        int pageCount=userCount/10;
        if(userCount%10!=0){
            pageCount++;
        }
        page.setPageCount(pageCount);
        return page;
    }

    @Override
    public int insertUser(User user) {
        return userMapper.insert(user);
    }

    @Override
    public int deleteUserById(int userId) {
        borrowingBooksMapper.deleteByUserId(userId);
        return userMapper.deleteByPrimaryKey(userId);
    }
}
